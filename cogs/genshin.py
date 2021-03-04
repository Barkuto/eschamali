
import os
import importlib
import random
from collections import Counter
from discord import Colour
from discord import Embed
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB
GS = importlib.import_module('.genshinstats', 'cogs')
LOGGER = UTILS.VARS.LOGGER

GENSHIN_TABLE = 'genshin'
GENSHIN_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
GENSHIN_TABLE_COL2 = ('genshin_id', DB_MOD.INTEGER)
GENSHIN_TABLE_COL3 = ('community_id', DB_MOD.INTEGER)
DB_PATH = os.path.join(os.path.dirname(__file__), 'genshinstats', 'genshin_binds.db')

NOT_BINDED_MSG = 'You have not binded a Genshin ID yet. See `!help bind` for details.'
NOT_PUBLIC_MSG = 'Your Hoyolab profile lab is not public.'

regions = {
    'os_usa': 'NA',
    'os_euro': 'EU',
    'os_asia': 'ASIA',
    'os_cht': 'SAR',
}


class Genshin(commands.Cog):
    """Displays Genshin Impact data"""

    def __init__(self, bot):
        self.bot = bot
        self.accounts = []
        self._init_account_cookies()
        self._init_db()

    def _init_db(self):
        db = DB(DB_PATH)
        db.create_table(GENSHIN_TABLE, GENSHIN_TABLE_COL1, GENSHIN_TABLE_COL2, GENSHIN_TABLE_COL3)

    def _init_account_cookies(self):
        with open('genshin_cookies') as f:
            lines = f.readlines()
            for l in lines:
                split = l.split(' ')
                self.accounts += [{'account_id': split[0], 'cookie_token':split[1]}]
        if not self.accounts:
            LOGGER.error('Invalid cookies file. Genshin cog will not work.')

    def _set_cookies(self):
        num = random.randint(0, len(self.accounts) - 1)
        GS.set_cookie(account_id=self.accounts[num]['account_id'], cookie_token=self.accounts[num]['cookie_token'])

    def _get_db(self):
        return DB(DB_PATH)

    def _get_genshin_id(self, user):
        db = DB(DB_PATH)
        gid = db.get_value(GENSHIN_TABLE, GENSHIN_TABLE_COL2[0], (GENSHIN_TABLE_COL1[0], user.id))
        cid = db.get_value(GENSHIN_TABLE, GENSHIN_TABLE_COL3[0], (GENSHIN_TABLE_COL1[0], user.id))
        return (gid, cid)

    def _get_avatar_url(self, name):
        AVATAR_URL = 'https://upload-os-bbs.mihoyo.com/game_record/genshin/character_icon/UI_AvatarIcon_%s.png'
        if name.lower() == 'hu tao':
            return AVATAR_URL % 'Hutao'
        return AVATAR_URL % name

    def _make_rarity_str(self, rarity):
        rarity_string = ''.join([':star:' for _ in range(rarity)])
        rarity_string += ''.join([':heavy_multiplication_x:' for _ in range(5 - rarity)])
        return rarity_string

    @commands.command(description='Bind Genshin ID to Discord account to see info of account',
                      help='genshin_id = In-game ID\ncommunity_id = Forum ID\nForum ID can be found from \"Account Info\" at https://www.hoyolab.com/',
                      brief='Bind Genshin ID')
    async def bind(self, ctx, genshin_id: int, community_id: int):
        if not UTILS.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        await ctx.message.delete()
        db = self._get_db()
        discord_match = db.get_rows(GENSHIN_TABLE, (GENSHIN_TABLE_COL1[0], ctx.author.id))
        genshin_match = db.get_rows(GENSHIN_TABLE, (GENSHIN_TABLE_COL2[0], genshin_id))
        community_match = db.get_rows(GENSHIN_TABLE, (GENSHIN_TABLE_COL3[0], community_id))
        msg = ctx.author.mention + ' '
        if discord_match:
            msg += 'You are already binded to a Genshin ID.'
        elif genshin_match:
            msg += 'You or someone else is already binded to that Genshin ID.'
        elif community_match:
            msg += 'You or someone else is already binded to that Community ID.'
        else:
            db.insert_row(GENSHIN_TABLE, (GENSHIN_TABLE_COL1[0], ctx.author.id), (GENSHIN_TABLE_COL2[0], genshin_id), (GENSHIN_TABLE_COL3[0], community_id))
            msg += 'Binded Genshin ID to your Discord account.'
        await ctx.send(msg)

    @commands.command(description='Unbind Genshin ID from Discord account',
                      help='Unbind Genshin ID from Discord account',
                      brief='Unbind Genshin ID')
    async def unbind(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        db = self._get_db()
        discord_match = db.get_rows(GENSHIN_TABLE, (GENSHIN_TABLE_COL1[0], ctx.author.id))
        msg = ctx.author.mention + ' '
        if discord_match:
            db.delete_rows(GENSHIN_TABLE, (GENSHIN_TABLE_COL1[0], ctx.author.id))
            msg += 'Unbinded your Discord account from Genshin ID.'
        else:
            msg += 'You are not binded to a Genshin ID.'
        await ctx.send(msg)

    @commands.command(description='See stats for your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.',
                      brief='See Genshin Stats',
                      aliases=['s'])
    async def stats(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        (gid, cid) = self._get_genshin_id(ctx.author)
        if not gid or not cid:
            return await ctx.send(NOT_BINDED_MSG)
        self._set_cookies()
        info = GS.fetch_endpoint('game_record/card/wapi/getGameRecordCard', uid=cid)
        stats = GS.get_user_info(gid)['stats']
        explorations = GS.fetch_endpoint('game_record/genshin/api/index', server=GS.recognize_server(gid), role_id=gid)['world_explorations']

        name = info['list'][0]['nickname']
        region = regions[info['list'][0]['region']]
        rank = info['list'][0]['level']
        achievements = stats['achievements']
        active_days = stats['active_days']
        characters = stats['characters']
        spiral_abyss = stats['spiral_abyss']
        anemoculi = stats['anemoculi']
        geoculi = stats['geoculi']
        common_chests = stats['common_chests']
        exquisite_chests = stats['exquisite_chests']
        precious_chests = stats['precious_chests']
        luxurious_chests = stats['luxurious_chests']
        unlocked_teleports = stats['unlocked_teleports']
        unlocked_domains = stats['unlocked_domains']

        e = Embed()
        e.title = name
        e.url = f'https://www.hoyolab.com/genshin/accountCenter/gameRecord?id={cid}'
        e.description = f'**Adventure Rank: {rank}**'
        e.add_field(name=f'`Region: {region}`', value='\n\n**Info:**', inline=False)

        e.add_field(name='Days Active', value=active_days)
        e.add_field(name='Achievements', value=achievements)
        e.add_field(name='Characters', value=characters)

        e.add_field(name='Spiral Abyss', value=spiral_abyss)
        e.add_field(name='Waypoints', value=unlocked_teleports)
        e.add_field(name='Domains', value=unlocked_domains)

        e.add_field(name='Anemoculi', value=anemoculi)
        e.add_field(name='Geoculi', value=geoculi)
        e.add_field(name='Common Chests', value=common_chests)

        e.add_field(name='Exquisite Chests', value=str(exquisite_chests) + '\n\n**World Exploration:**')
        e.add_field(name='Precious Chests', value=precious_chests)
        e.add_field(name='Luxurious Chests', value=luxurious_chests)

        for area in explorations:
            value = '{} Lv{}\n'.format(area['type'], area['level']) + 'Exploration: {}%'.format(area['exploration_percentage'] / 10.0)
            e.add_field(name=area['name'], value=value)

        await ctx.send(embed=e)

    @commands.command(description='See stats for your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.',
                      brief='See Genshin Stats',
                      aliases=['characters', 'chars', 'char', 'c'])
    async def character(self, ctx, *, query=None):
        if not UTILS.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        (gid, cid) = self._get_genshin_id(ctx.author)
        if not gid or not cid:
            return await ctx.send(NOT_BINDED_MSG)
        self._set_cookies()
        info = GS.fetch_endpoint('game_record/card/wapi/getGameRecordCard', uid=cid)
        try:
            characters = GS.get_all_characters(gid)
        except GS.errors.DataNotPublic:
            return await ctx.send(NOT_PUBLIC_MSG)

        name = info['list'][0]['nickname']

        e = Embed()

        if not query:
            e.title = name
            e.description = f'**Characters - {len(characters)}**\n'
            e.description += '```'
            per_line = 3
            char_pattern = '{:<10}{:<3}{:<1}|'
            for _ in range(per_line):
                e.description += char_pattern.format('Name', 'LV', 'C')
            e.description = e.description[:-1]
            e.description += '\n'
            count = 0
            for c in characters:
                if count >= per_line:
                    count = 0
                    e.description = e.description[:-1]
                    e.description += '\n'
                e.description += char_pattern.format(c['name'], c['level'], c['constellation'])
                count += 1
            e.description = e.description[:-1]
            e.description += '```'
        else:
            char = None
            for c in characters:
                names = [c['name'].lower(), c['name'].lower().replace(' ', '')]
                if query in names:
                    char = c
                    break
            if char:
                weapon = char['weapon']
                artifacts = char['artifacts']

                e.title = char['name'] + ' ' + ''.join([':star:' for _ in range(char['rarity'])])
                e.description = char['element']

                e.add_field(name='Level', value=char['level'])
                e.add_field(name='Constellation', value=char['constellation'])
                e.add_field(name='Friendship', value=char['friendship'])

                weapon_info = weapon['name'] + ' ' + ''.join([':star:' for _ in range(char['rarity'])]) + \
                    '\nLevel {}, Refinement Rank {}'.format(weapon['level'], weapon['refinement'])
                e.add_field(name='Weapon', value=weapon_info)

                art_info = ''
                art_names = ['', '', '', '', '']
                art_rarities = [0, 0, 0, 0, 0]
                art_lvls = [0, 0, 0, 0, 0]
                art_sets = [None, None, None, None, None]
                for a in artifacts:
                    art_names[a['position_index'] - 1] = a['set']['name']
                    art_rarities[a['position_index'] - 1] = a['rarity']
                    art_lvls[a['position_index'] - 1] = a['level']
                    art_sets[a['position_index'] - 1] = a['set']
                art_info += f'{self._make_rarity_str(art_rarities[0])} `Lv{art_lvls[0]} Flower    : {art_names[0]}`\n'
                art_info += f'{self._make_rarity_str(art_rarities[1])} `Lv{art_lvls[1]} Feather   : {art_names[1]}`\n'
                art_info += f'{self._make_rarity_str(art_rarities[2])} `Lv{art_lvls[2]} Timepiece : {art_names[2]}`\n'
                art_info += f'{self._make_rarity_str(art_rarities[3])} `Lv{art_lvls[3]} Goblet    : {art_names[3]}`\n'
                art_info += f'{self._make_rarity_str(art_rarities[4])} `Lv{art_lvls[4]} Circlet   : {art_names[4]}`'

                counts = Counter(art_names)
                effects = []
                for set_name, count in counts.items():
                    if set_name:
                        if count < 2:
                            pass
                        elif count <= 3:
                            effects += [art_sets[art_names.index(set_name)]['effects'][0]]
                        elif count <= 5:
                            effects += [art_sets[art_names.index(set_name)]['effects'][0]]
                            effects += [art_sets[art_names.index(set_name)]['effects'][1]]
                effects_info = '\n'.join([f"`{e['pieces']} Piece:` " + e['effect'] for e in effects])
                if not effects:
                    effects_info = 'None.'

                e.add_field(name='Artifacts', value=art_info, inline=False)
                e.add_field(name='Effects', value=effects_info, inline=False)

                e.set_thumbnail(url=self._get_avatar_url(char['name']))
                e.set_footer(text=name)

        await ctx.send(embed=e)


def setup(bot):
    bot.add_cog(Genshin(bot))
