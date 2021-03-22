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
