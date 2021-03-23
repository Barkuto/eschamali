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
        self.house_cards = [self.deck.pop(0), self.deck.pop(1)]
        self.player_cards = [[self.deck.pop(0), self.deck.pop(0)]]
        self.turn = PLAYER
        self.states = [ONGOING]

        self.house_user = house_user
        self.player_user = player_user

        self.credits.transfer_from_to(player_user, house_user, bet)

        self._determine_state()

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

            num_house_cards = len(self.house_cards)
            num_player_cards = len(curr_player_hand)

            # Beginning State, 2 cards both sides
            if num_house_cards == num_player_cards == 2:
                # Handle auto win when house or player dealt 21
                if house_sum == player_sum == 21:
                    self.set_curr_state(DRAW)
                    # Return Bet
                    self.credits.transfer_from_to(self.house_user, self.player_user, curr_bet)
                elif player_sum == 21:
                    self.set_curr_state(PLAYER_WIN)
                    # Bet Payout 3:2
                    self.credits.transfer_from_to(self.house_user, self.player_user, curr_bet + curr_bet / 2 * 3)
                elif house_sum == 21:
                    self.set_curr_state(PLAYER_LOSE)
            # Player Turn
            elif self.turn == PLAYER:
                if player_sum > 21:
                    # When player busts, move to next hand
                    # Or make house hit/hold
                    # Then process House Turn
                    # This is self.hold() with a condition to hit()
                    self.curr_hand += 1
                    if self.curr_hand == len(self.player_cards):
                        self.turn = HOUSE
                        if len(self.player_cards) > 1:
                            self.hit()
                        else:
                            self._determine_state()
        elif curr_state == PLAYER_DONE:
            # House Turn
            for i in range(len(self.player_cards)):
                curr_bet = self.bets[i]
                curr_player_sum = best_sum(self.player_cards[i])
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
                    self.credits.transfer_from_to(self.house_user, self.player_user, 2 * curr_bet)
                elif self.states[i] == DRAW:
                    self.credits.transfer_from_to(self.house_user, self.player_user, curr_bet)

    def hit(self):
        if self.turn == HOUSE:
            house_sum = best_sum(self.house_cards)
            while house_sum < THRESHOLD:
                self.house_cards += [self.deck.pop(0)]
                house_sum = best_sum(self.house_cards)
        elif self.turn == PLAYER:
            self.player_cards[self.curr_hand] += [self.deck.pop(0)]
        self._determine_state()

    def hold(self):
        self.curr_hand += 1
        if self.curr_hand == len(self.player_cards):
            self.turn = HOUSE
            self.hit()

    def double(self):
        if self.turn != PLAYER:
            return
        curr_index = self.curr_hand
        if not self.doubled[curr_index] and len(self.player_cards[curr_index]) == 2:
            player_creds = self.credits.get_user_creds(self.player_user)
            if player_creds >= self.bets[curr_index]:
                self.credits.transfer_from_to(self.player_user, self.house_user, self.bets[curr_index])
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
                self.player_cards[self.curr_hand] = [curr_cards[0], self.deck.pop(0)]
                self.player_cards += [[curr_cards[1], self.deck.pop(0)]]

                self.bets += [self.bets[self.curr_hand]]
                self.doubled += [False]
                self.states += [ONGOING]

                self.credits.transfer_from_to(self.player_user, self.house_user, self.bets[self.curr_hand])
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
