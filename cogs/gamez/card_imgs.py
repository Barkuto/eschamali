import os
import importlib
from PIL import Image, ImageFont, ImageDraw
from cogs.gamez import blackjack as bj

"""
Card Sheet Methods
"""
SETS_PATH = os.path.join(os.path.dirname(__file__), 'sets')
FONTS_PATH = os.path.join(os.path.dirname(__file__), 'fonts')
CUSTOM_PATH = os.path.join(SETS_PATH, 'custom')

DEFAULT_SET = 'default'
JAPAN_SET = 'japan'
SETS = [DEFAULT_SET, JAPAN_SET]


def reload():
    importlib.reload(bj)


def load_sheet_img(sheet_name, blank=False):
    if str(sheet_name).lower() in SETS:
        return Image.open(fp=os.path.join(SETS_PATH, sheet_name, f'sheet{"_blank" if blank else ""}.png'))
    try:
        return Image.open(fp=os.path.join(CUSTOM_PATH, f'{sheet_name}{"_blank" if blank else ""}.png'))
    except:
        return Image.open(fp=os.path.join(SETS_PATH, DEFAULT_SET, f'sheet{"_blank" if blank else ""}.png'))


def make_set_sheet_img(set_name=DEFAULT_SET):
    return make_sheet_img(set_name, set_name, set_name, set_name, set_name, set_name)


def make_sheet_img(base_name=DEFAULT_SET, border_name=DEFAULT_SET, suits_name=DEFAULT_SET, black_name=DEFAULT_SET, red_name=DEFAULT_SET, back_name=DEFAULT_SET):
    base_img = Image.open(fp=os.path.join(SETS_PATH, base_name, 'base.png'))
    border_img = Image.open(fp=os.path.join(SETS_PATH, border_name, 'border.png'))
    back_img = Image.open(fp=os.path.join(SETS_PATH, back_name, 'back.png'))

    black_num_path = os.path.join(SETS_PATH, black_name, 'black')
    red_num_path = os.path.join(SETS_PATH, red_name, 'red')
    suits_path = os.path.join(SETS_PATH, suits_name, 'suits')

    base_width = base_img.size[0]
    base_height = base_img.size[1]
    sheet_img = Image.new('RGBA', (base_width * 13, base_height * 5))
    sheet_blank_img = Image.new('RGBA', (base_width * 13, base_height * 5))

    suits = ['spades', 'clubs', 'diamonds', 'hearts']
    nums = ['a', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'j', 'q', 'k']
    for y in range(0, sheet_img.size[1] - base_height, base_height):
        suit_index = y // base_height
        suit_img = Image.open(os.path.join(suits_path, suits[suit_index] + '.png'))
        for x in range(0, sheet_img.size[0], base_width):
            num_index = x // base_width
            num_path = black_num_path if suit_index <= 1 else red_num_path
            num_img = Image.open(os.path.join(num_path, nums[num_index] + '.png'))
            sheet_img.paste(base_img, (x, y))
            sheet_img.paste(border_img, (x, y), border_img)
            sheet_img.paste(suit_img, (x, y), suit_img)
            sheet_img.paste(num_img, (x, y), num_img)

            sheet_blank_img.paste(base_img, (x, y))
            sheet_blank_img.paste(border_img, (x, y), border_img)
            sheet_blank_img.paste(num_img, (x, y + base_height // 4), num_img)
    sheet_img.paste(back_img, (0, base_height * 4), back_img)
    sheet_blank_img.paste(back_img, (0, base_height * 4), back_img)

    return (sheet_img, sheet_blank_img)


"""
Game Specific Image Methods
"""

# Blackjack


def make_bj_hand_img(sheet_img, cards, unknown=False):
    card_width = sheet_img.size[0] // 13
    card_height = sheet_img.size[1] // 5
    card_spacing = 1
    hand_img = Image.new('RGBA', ((card_width + card_spacing) * len(cards), card_height))
    for i in range(len(cards)):
        if len(cards) == 2 and i == 1 and unknown:
            card_img = sheet_img.crop((0, 4 * card_height, card_width, 5 * card_height))
        else:
            c = cards[i]
            num = suit = 0
            if isinstance(c, tuple):
                num = c[0]
                suit = c[1]
            else:
                num = c
            if num == 'Ace':
                num = 0
            elif num == 'Jack':
                num = 10
            elif num == 'Queen':
                num = 11
            elif num == 'King':
                num = 12
            else:
                num = int(num) - 1
            if suit == 'Spades':
                suit = 0
            elif suit == 'Clubs':
                suit = 1
            elif suit == 'Diamonds':
                suit = 2
            elif suit == 'Hearts':
                suit = 3
            card_img = sheet_img.crop((num * card_width,
                                       suit * card_height,
                                       num * card_width + card_width,
                                       suit * card_height + card_height))
        hand_img.paste(card_img, (i * card_width + i * card_spacing, 0), card_img)
    return hand_img


def make_bj_img(cog, user, blackjack, sheet_img):
    user_creds = cog.cr.get_user_creds(user)
    bets = blackjack.bets
    bot_hand = blackjack.house_cards
    user_hands = blackjack.player_cards
    results = blackjack.states

    font = ImageFont.truetype(os.path.join(FONTS_PATH, 'consolaz.ttf'), 16)

    if blackjack.get_curr_state() != bj.ONGOING:
        bot_hand_img = make_bj_hand_img(sheet_img, bot_hand)
        bot_name = cog._cards_to_embed_sum_name(cog.bot.user, bot_hand)
    else:
        bot_hand_img = make_bj_hand_img(sheet_img, bot_hand, unknown=True)
        bot_name = cog._cards_to_embed_sum_name(cog.bot.user, bot_hand[:1], unknown=True)
    user_hand_imgs = [make_bj_hand_img(sheet_img, h) for h in user_hands]
    user_name = f'{user.name} ({"/".join([str(bj.best_sum(c)) for c in user_hands])})'

    test_img = Image.new('RGB', (1, 1))
    test_draw = ImageDraw.Draw(test_img)
    bot_name_width, _ = test_draw.textsize(bot_name, font=font)
    user_name_width, _ = test_draw.textsize(user_name, font=font)

    padding_x = 5
    padding_y = 5
    hand_spacing = 4
    text_height = 20

    img_width = sheet_img.size[0] // 13
    img_height = padding_y * 3 + bot_hand_img.size[1] + text_height * 2 + hand_spacing * (len(user_hands) - 1)
    for i in user_hand_imgs:
        img_width = max(img_width, i.size[0])
        img_height += i.size[1]
    img_width = max(img_width, bot_hand_img.size[0], bot_name_width, user_name_width)
    img_width += 2 * padding_x

    img = Image.new('RGBA', (img_width, img_height), (0, 100, 0, 0))
    draw = ImageDraw.Draw(img)

    img.paste(bot_hand_img, (padding_x, padding_y + text_height), bot_hand_img)
    ypos = bot_hand_img.size[1] + text_height * 2 + padding_y * 2
    for i in user_hand_imgs:
        img.paste(i, (padding_x, ypos), i)
        ypos += i.size[1] + hand_spacing

    if len(user_hands) > 1 and blackjack.curr_hand < len(user_hands):
        outline_width = 2
        curr_hand_x = padding_x - outline_width
        curr_hand_y = padding_y * 2 + text_height * 2 + bot_hand_img.size[1] + hand_spacing * blackjack.curr_hand + user_hand_imgs[blackjack.curr_hand].size[1] * blackjack.curr_hand - outline_width
        curr_width = user_hand_imgs[blackjack.curr_hand].size[0]
        curr_height = user_hand_imgs[blackjack.curr_hand].size[1]
        draw.line([(curr_hand_x, curr_hand_y-1),
                   (curr_hand_x+curr_width+outline_width+1, curr_hand_y-1),
                   (curr_hand_x+curr_width+outline_width+1, curr_hand_y+curr_height+outline_width*2),
                   (curr_hand_x, curr_hand_y+curr_height+outline_width*2),
                   (curr_hand_x, curr_hand_y)],
                  fill=(0, 0, 0), width=outline_width)

    stroke_width = 1
    stroke_fill = (255, 255, 255)
    text_fill = (0, 0, 0)
    draw.text((padding_x, padding_y), bot_name, text_fill, font=font, stroke_width=stroke_width, stroke_fill=stroke_fill)
    draw.text((padding_x, padding_y * 2 + text_height + bot_hand_img.size[1]), user_name, text_fill, font=font, stroke_width=stroke_width, stroke_fill=stroke_fill)

    return img
