import threading
import random

ONGOING = 0
PLAYER_WIN = 1
PLAYER_LOSE = 2
PLAYER_BUST = 3
HOUSE_BUST = 4
DRAW = 5
PLAYER_DONE = 6

HIT = 11
HOLD = 12
DOUBLE = 13
SPLIT = 14

HOUSE = 'HOUSE'
PLAYER = 'PLAYER'

INVALID_BET = 'Invalid Bet'
INSUFFICIENT_CREDITS = 'Insufficient Credits'
INVALID_DOUBLE = 'Cannot Double Down'
INVALID_SPLIT = 'Cannot Split'

THRESHOLD = 17


class BlackjackException(Exception):
    pass


class InvalidBetException(BlackjackException):
    pass


class InsufficientCreditsException(BlackjackException):
    pass


class InvalidDoubleStateException(BlackjackException):
    pass


class InvalidSplitStateException(BlackjackException):
    pass


class Blackjack():
    def __init__(self, credits, deck, bet, house_user, player_user):
        if credits.get_user_creds(player_user) < bet:
            raise InsufficientCreditsException(INSUFFICIENT_CREDITS)

        bet_test = bet / 2 * 3
        if bet <= 0 or (bet_test - int(bet_test) != 0):
            raise InvalidBetException(INVALID_BET)

        self.credits = credits
        self.lock = threading.Lock()
        self.bets = [bet]
        self.doubled = [False]
        self.curr_hand = 0
        self.deck = deck
        self.deck.lock.acquire()
        self.house_cards = [self.deck.draw(), self.deck.draw(1)]
        self.player_cards = [[self.deck.draw(), self.deck.draw()]]
        self.deck.lock.release()
        self.turn = PLAYER
        self.states = [ONGOING]
        self.net = -bet

        self.house_user = house_user
        self.player_user = player_user

        self.credits.transfer_from_to(player_user, house_user, bet)

        self._determine_state()

    def calc_32(self, bet):
        payout = int(bet / 2 * 3)
        if payout % 2 == 0:
            return payout
        return payout + 1

    def get_states(self):
        return self.states

    def get_curr_state(self):
        if self.curr_hand == len(self.states):
            return PLAYER_DONE
        return self.states[self.curr_hand]

    def set_state(self, state, index):
        if state in [ONGOING, PLAYER_WIN, PLAYER_LOSE,
                     PLAYER_BUST, HOUSE_BUST, DRAW]:
            self.states[index] = state

    def set_curr_state(self, state):
        self.set_state(state, self.curr_hand)

    def _determine_state(self):
        curr_state = self.get_curr_state()
        house_sum = best_sum(self.house_cards)
        if curr_state == ONGOING:
            curr_bet = self.bets[self.curr_hand]
            curr_player_hand = self.player_cards[self.curr_hand]
            player_sum = best_sum(curr_player_hand)
            num_player_cards = len(curr_player_hand)

            # Beginning State, 2 cards both sides
            # if num_house_cards == num_player_cards == 2:
            if num_player_cards == 2:
                # Handle auto win when house or player dealt 21
                if house_sum == player_sum == 21 or player_sum == 21:
                    self.set_curr_state(PLAYER_WIN)
                elif house_sum == 21:
                    self.set_curr_state(PLAYER_LOSE)
                else:
                    return
                self.curr_hand += 1
                self._determine_state()
            # Player Turn
            elif self.turn == PLAYER:
                if player_sum > 21:
                    # When player busts, Move to next hand
                    self.curr_hand += 1
                    # If all player hands are done
                    if self.curr_hand == len(self.player_cards):
                        # Change turn to house
                        self.turn = HOUSE
                        # If player split, make house hit to have a hand to compare
                        if len(self.player_cards) > 1:
                            self.hit()
                        # If player not split, continue/end game
                        else:
                            self._determine_state()
                    # If not all player hands done, continue player turn
                    else:
                        self._determine_state()
        elif curr_state == PLAYER_DONE:
            # House Turn
            for i in range(len(self.player_cards)):
                curr_bet = self.bets[i]
                curr_player_sum = best_sum(self.player_cards[i])
                if self.states[i] == ONGOING:
                    if curr_player_sum > 21 and house_sum > 21:
                        self.set_state(DRAW, i)
                    elif curr_player_sum > 21:
                        self.set_state(PLAYER_BUST, i)
                    elif house_sum > 21:
                        self.set_state(HOUSE_BUST, i)
                    elif house_sum > curr_player_sum:
                        self.set_state(PLAYER_LOSE, i)
                    elif house_sum < curr_player_sum:
                        self.set_state(PLAYER_WIN, i)
                    elif house_sum == curr_player_sum:
                        self.set_state(DRAW, i)

                if self.states[i] in [HOUSE_BUST, PLAYER_WIN]:
                    is_two = len(self.player_cards[i]) == 2
                    is_blackjack = house_sum == curr_player_sum == 21 or curr_player_sum == 21
                    if self.states[i] == PLAYER_WIN and is_two and is_blackjack:
                        # Bet Payout 3:2
                        self.net += curr_bet + self.calc_32(curr_bet)
                    else:
                        self.net += curr_bet + curr_bet
                elif self.states[i] == DRAW:
                    self.net += curr_bet
            # End of Game, distribute winnings
            self.credits.transfer_from_to(self.house_user, self.player_user, self.net + sum(self.bets))

    def hit(self):
        self.deck.lock.acquire()
        if self.turn == HOUSE:
            house_sum = best_sum(self.house_cards)
            while house_sum < THRESHOLD:
                self.house_cards += [self.deck.draw()]
                house_sum = best_sum(self.house_cards)
        elif self.turn == PLAYER:
            self.player_cards[self.curr_hand] += [self.deck.draw()]
        self.deck.lock.release()
        self._determine_state()

    def hold(self):
        self.curr_hand += 1
        if self.curr_hand == len(self.player_cards):
            self.turn = HOUSE
            self.hit()
        else:
            self._determine_state()

    def double(self):
        if self.turn != PLAYER:
            return
        curr_index = self.curr_hand
        if not self.doubled[curr_index] and len(self.player_cards[curr_index]) == 2:
            player_creds = self.credits.get_user_creds(self.player_user)
            if player_creds >= self.bets[curr_index]:
                self.credits.transfer_from_to(self.player_user, self.house_user, self.bets[curr_index])
                self.net -= self.bets[curr_index]
                self.doubled[curr_index] = True
                self.bets[curr_index] *= 2
                self.hit()
                if self.states[curr_index] == ONGOING:
                    self.hold()
            else:
                raise InsufficientCreditsException(INSUFFICIENT_CREDITS + ' to Double Down')
        else:
            raise InvalidDoubleStateException(INVALID_DOUBLE)

    def split(self):
        if self.turn != PLAYER:
            return
        curr_cards = self.player_cards[self.curr_hand]
        if len(curr_cards) == 2 and curr_cards[0] == curr_cards[1]:
            user_creds = self.credits.get_user_creds(self.player_user)
            curr_bet = self.bets[self.curr_hand]
            if user_creds >= curr_bet:
                self.deck.lock.acquire()
                self.player_cards[self.curr_hand] = [curr_cards[0], self.deck.draw()]
                self.player_cards += [[curr_cards[1], self.deck.draw()]]
                self.deck.lock.release()

                self.bets += [self.bets[self.curr_hand]]
                self.doubled += [False]
                self.states += [ONGOING]

                self.credits.transfer_from_to(self.player_user, self.house_user, self.bets[self.curr_hand])
                self.net -= self.bets[self.curr_hand]

                self._determine_state()
            else:
                raise InsufficientCreditsException(INSUFFICIENT_CREDITS + ' to Split')
        else:
            raise InvalidSplitStateException(INVALID_SPLIT)


def calc_sums(cards):
    nums = []
    num_aces = 0
    for c in cards:
        if not c == 'Ace':
            if c in ['Jack', 'Queen', 'King']:
                nums += [10]
            else:
                nums += [int(c)]
        else:
            num_aces += 1
    base_sum = sum(nums)
    low_sum = base_sum
    high_sum = base_sum
    for _ in range(num_aces):
        low_sum += 1
    for _ in range(num_aces):
        if high_sum <= 10:
            high_sum += 11
        else:
            high_sum += 1
    if low_sum == high_sum:
        return (low_sum,)
    return (low_sum, high_sum)


def best_sum(cards):
    sums = calc_sums(cards)
    if len(sums) > 1 and sums[1] <= 21:
        return sums[1]
    return sums[0]
