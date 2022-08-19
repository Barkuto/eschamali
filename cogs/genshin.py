import importlib


import importlib
import os
import random
import cogs.genshinstats as gs
from collections import Counter
from datetime import datetime
from discord import Embed
from discord.ext import commands

NOT_BINDED_MSG = 'You have not binded a Genshin ID yet. See `!help bind` for details.'
NOT_PUBLIC_MSG = 'Your Hoyolab profile lab is not public.'


class Genshin(commands.Cog):
    """Displays Genshin Impact data"""

    def __init__(self, bot):
        importlib.reload(gs)

        self.GENSHIN_TABLE = 'genshin'
        self.GENSHIN_TABLE_COL1 = ('discord_id', bot.db.INTEGER)
        self.GENSHIN_TABLE_COL2 = ('genshin_id', bot.db.INTEGER)
        self.GENSHIN_TABLE_COL3 = ('community_id', bot.db.INTEGER)

        self.REGIONS = {
            'os_usa': 'NA',
            'os_euro': 'EU',
            'os_asia': 'ASIA',
            'os_cht': 'SAR',
        }

        self.bot = bot
        self.accounts = []
        self._init_account_cookies()
        self._init_db()

    def _init_db(self):
        self.db = self.bot.db.DB(os.path.join(os.path.dirname(__file__), 'genshinstats', 'genshin_binds.db'))
        self.db.create_table(self.GENSHIN_TABLE, self.GENSHIN_TABLE_COL1, self.GENSHIN_TABLE_COL2, self.GENSHIN_TABLE_COL3)

    def _init_account_cookies(self):
        with open('genshin_cookies') as f:
            lines = f.readlines()
            for l in lines:
                split = l.split(' ')
                self.accounts += [{'account_id': split[0], 'cookie_token':split[1]}]
        if not self.accounts:
            self.bot.vars.LOGGER.error('Invalid cookies file. Genshin cog will not work.')

    def _set_cookies(self):
        num = random.randint(0, len(self.accounts) - 1)
        gs.set_cookie(account_id=self.accounts[num]['account_id'], cookie_token=self.accounts[num]['cookie_token'])

    def _get_genshin_id(self, user):
        gid = self.db.get_value(self.GENSHIN_TABLE, self.GENSHIN_TABLE_COL2[0], (self.GENSHIN_TABLE_COL1[0], user.id))
        cid = self.db.get_value(self.GENSHIN_TABLE, self.GENSHIN_TABLE_COL3[0], (self.GENSHIN_TABLE_COL1[0], user.id))
        return (gid, cid)

    def _get_avatar_url(self, name):
        AVATAR_URL = 'https://upload-os-bbs.mihoyo.com/game_record/genshin/character_icon/UI_AvatarIcon_%s.png'
        if name.lower() == 'hu tao':
            return AVATAR_URL % 'Hutao'
        elif name.lower() == 'noelle':
            return AVATAR_URL % 'Noel'
        return AVATAR_URL % name

    def _make_rarity_str(self, rarity, max_rarity=5):
        rarity_string = ''.join([':star:' for _ in range(rarity)])
        rarity_string += ''.join([':heavy_multiplication_x:' for _ in range(max_rarity - rarity)])
        return rarity_string

    @commands.command(description='Bind Genshin ID to Discord account to see info of account',
                      help='genshin_id = In-game ID\ncommunity_id = Forum ID\nForum ID can be found from \"Account Info\" at https://www.hoyolab.com/',
                      brief='Bind Genshin ID')
    async def bind(self, ctx, genshin_id: int, community_id: int):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        await ctx.message.delete()
        discord_match = self.db.get_rows(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL1[0], ctx.author.id))
        genshin_match = self.db.get_rows(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL2[0], genshin_id))
        community_match = self.db.get_rows(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL3[0], community_id))
        msg = ctx.author.mention + ' '
        if discord_match:
            msg += 'You are already binded to a Genshin ID.'
        elif genshin_match:
            msg += 'You or someone else is already binded to that Genshin ID.'
        elif community_match:
            msg += 'You or someone else is already binded to that Community ID.'
        else:
            self.db.insert_row(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL1[0], ctx.author.id), (self.GENSHIN_TABLE_COL2[0], genshin_id), (self.GENSHIN_TABLE_COL3[0], community_id))
            msg += 'Binded Genshin ID to your Discord account.'
        await ctx.send(msg)

    @commands.command(description='Unbind Genshin ID from Discord account',
                      help='Unbind Genshin ID from Discord account',
                      brief='Unbind Genshin ID')
    async def unbind(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        discord_match = self.db.get_rows(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL1[0], ctx.author.id))
        msg = ctx.author.mention + ' '
        if discord_match:
            self.db.delete_rows(self.GENSHIN_TABLE, (self.GENSHIN_TABLE_COL1[0], ctx.author.id))
            msg += 'Unbinded your Discord account from Genshin ID.'
        else:
            msg += 'You are not binded to a Genshin ID.'
        await ctx.send(msg)

    @commands.command(description='See stats for your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.',
                      brief='See Genshin Stats',
                      aliases=['s'])
    async def stats(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        (gid, cid) = self._get_genshin_id(ctx.author)
        if not gid or not cid:
            return await ctx.send(NOT_BINDED_MSG)
        self._set_cookies()
        try:
            info = gs.fetch_endpoint('game_record/card/wapi/getGameRecordCard', uid=cid)
            stats = gs.get_user_info(gid)['stats']
            explorations = gs.fetch_endpoint('game_record/genshin/api/index', server=gs.recognize_server(gid), role_id=gid)['world_explorations']
        except gs.errors.DataNotPublic:
            return await ctx.send(NOT_PUBLIC_MSG)

        name = info['list'][0]['nickname']
        region = self.REGIONS[info['list'][0]['region']]
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

    @commands.command(description='See characters of your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.',
                      brief='See Genshin Characters',
                      aliases=['characters', 'chars', 'char', 'c'])
    async def character(self, ctx, *, query=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        (gid, cid) = self._get_genshin_id(ctx.author)
        if not gid or not cid:
            return await ctx.send(NOT_BINDED_MSG)
        self._set_cookies()
        info = gs.fetch_endpoint('game_record/card/wapi/getGameRecordCard', uid=cid)
        try:
            characters = gs.get_all_characters(gid)
        except gs.errors.DataNotPublic:
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

    @commands.command(description='See abyss info for your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.\nNo parameters to see stats, use a number(9,10,etc) to see teams for the floor.',
                      brief='See Genshin Abyss Info',
                      aliases=['a', 'spiralabyss', 'sa'])
    async def abyss(self, ctx, query: int = None):
        await self._abyss(ctx, query)

    @commands.command(description='See previous abyss info for your Genshin Account',
                      help='Need to use the bind command to bind IDs to Discord account first.\nNo parameters to see stats, use a number(9,10,etc) to see teams for the floor.',
                      brief='See Previous Genshin Abyss Info',
                      aliases=['pa', 'prevspiralabyss', 'psa'])
    async def prevabyss(self, ctx, query: int = None):
        await self._abyss(ctx, query, True)

    async def _abyss(self, ctx, query: int = None, previous=False):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not self.accounts:
            return
        (gid, cid) = self._get_genshin_id(ctx.author)
        if not gid or not cid:
            return await ctx.send(NOT_BINDED_MSG)
        self._set_cookies()
        info = gs.fetch_endpoint('game_record/card/wapi/getGameRecordCard', uid=cid)
        try:
            spiral_abyss = gs.get_spiral_abyss(gid, previous=previous)
        except gs.errors.DataNotPublic:
            return await ctx.send(NOT_PUBLIC_MSG)

        name = info['list'][0]['nickname']

        e = Embed()
        e.title = f"Spiral Abyss Season {spiral_abyss['season']}"

        if not query:
            start_time = datetime.fromtimestamp(spiral_abyss['season_start_time']).strftime('%b %d, %Y %I:%M %p')
            end_time = datetime.fromtimestamp(spiral_abyss['season_end_time']).strftime('%b %d, %Y %I:%M %p')
            e.description = f"{start_time} - {end_time}\n"

            stats = "Total Battles: {}\nTotal Wins: {}\nMax Floor: {}\nTotal Stars: {}".format(
                spiral_abyss['stats']['total_battles'], spiral_abyss['stats']['total_wins'],
                spiral_abyss['stats']['max_floor'], spiral_abyss['stats']['total_stars'])
            e.add_field(name='Stats', value=stats)

            ranks = spiral_abyss['character_ranks']
            names = ['Most Chambers Won', 'Most Chambers Lost',
                     'Most Damage Taken', 'Most Bursts Used',
                     'Most Skills Used', 'Strongest Hit']
            info_stats = [ranks['most_chambers_won'][:4], ranks['most_chambers_lost'][:4],
                          ranks['most_damage_taken'][:4], ranks['most_bursts_used'][:4],
                          ranks['most_skills_used'][:4], ranks['strongest_hit'][:4]]

            info_stats = ['\n'.join(['{:<10}({})'.format(o['name'], o['value']) for o in stat]) for stat in info_stats]
            info_stats = [s if s else 'No Info' for s in info_stats]
            for i in range(len(names) - 1):
                e.add_field(name=names[i], value=info_stats[i])
            e.add_field(name=names[5], value=info_stats[5], inline=False)

            floors = spiral_abyss['floors']
            fl9 = fl10 = fl11 = fl12 = None
            for f in floors:
                if f['floor'] == 9:
                    fl9 = f
                elif f['floor'] == 10:
                    fl10 = f
                elif f['floor'] == 11:
                    fl11 = f
                elif f['floor'] == 12:
                    fl12 = f
            names = ['Floor 9', 'Floor 10', 'Floor 11', 'Floor 12']
            floor_infos = [fl9, fl10, fl11, fl12]
            for i in range(len(floor_infos)):
                if floor_infos[i]:
                    info = '\n'.join(['`Chamber {}:` {}'.format(l['chamber'], self._make_rarity_str(l['stars'], 3))
                                      for l in floor_infos[i]['levels']])
                    floor_infos[i] = info
                else:
                    floor_infos[i] = 'Not yet attempted.'
            e.add_field(name=names[0], value=floor_infos[0])
            e.add_field(name=names[1], value=floor_infos[1])
            e.add_field(name='\u200b', value='\u200b')
            e.add_field(name=names[2], value=floor_infos[2])
            e.add_field(name=names[3], value=floor_infos[3])
            e.add_field(name='\u200b', value='\u200b')
        else:
            floors = spiral_abyss['floors']
            found_floor = [f for f in floors if f['floor'] == query]
            if found_floor:
                found_floor = found_floor[0]
                e.description = f"**Floor {found_floor['floor']}**\n"

                chambers = found_floor['levels']
                e.description += '\n'.join([f"`Chamber {s['chamber']}:`" + self._make_rarity_str(s['stars'], 3) for s in chambers])

                info = '`Chamber Half {:<50}\n'.format('Team')
                for c in chambers:
                    for b in c['battles']:
                        info += '{:^7} {:^4} {:<50}\n'.format(
                            c['chamber'], b['half'],
                            '/'.join([chars['name'] for chars in b['characters']]))
                e.add_field(name='Info', value=info + '`')
            else:
                return await ctx.send('There is no data for that floor in this season yet.')

        e.set_footer(text=name)
        await ctx.send(embed=e)


async def setup(bot):
    await bot.add_cog(Genshin(bot))
