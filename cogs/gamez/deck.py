import threading
import random


def make_deck(existing_cards=[], with_suits=False, num_decks=1):
    nums = ['Ace', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'Jack', 'Queen', 'King']
    suits = ['Spades', 'Clubs', 'Diamonds', 'Hearts']
    deck = []
    for s in suits:
        for n in nums:
            if with_suits:
                deck += [(n, s)] * num_decks
            else:
                deck += [n] * num_decks
    for c in existing_cards:
        deck.remove(c)
    random.shuffle(deck)
    return deck


class Deck():
    def __init__(self, existing_cards=[], with_suits=False, num_decks=1):
        self.lock = threading.Lock()
        self.existing = existing_cards
        self.suits = with_suits
        self.num_decks = num_decks
        self.deck = make_deck(existing_cards, with_suits, num_decks)

    def draw(self, index=0):
        return self.deck.pop(index)

    def reshuffle(self):
        self.deck = make_deck(self.existing, self.suits, self.num_decks)

    def cards_used(self):
        return (self.num_decks * 52) - self.cards_left()

    def cards_left(self):
        return len(self.deck)
