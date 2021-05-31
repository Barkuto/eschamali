import importlib
import asyncio
import random
import discord.utils
from discord import Colour
from discord import Embed
from discord.ext import tasks, commands
from concurrent.futures import ThreadPoolExecutor
from enum import Enum
from math import ceil, floor
from datetime import datetime

UTILS = importlib.import_module('.utils', 'util')
PAD_DATA = importlib.import_module('.pad_data', 'cogs.pad_data')
LOGGER = UTILS.VARS.LOGGER

Attribute = PAD_DATA.Attribute
Type = PAD_DATA.Type
Awakening = PAD_DATA.Awakening

NA = PAD_DATA.NA
JP = PAD_DATA.JP

LEFT_ARROW = '⬅️'
RIGHT_ARROW = '➡️'
REGIONAL_INDICATOR_N = '🇳'
REGIONAL_INDICATOR_J = '🇯'
X = '❌'
LEFT_TRIANGLE = '◀️'
RIGHT_TRIANGLE = '▶️'

PDX_LINK = 'http://puzzledragonx.com/en/monster.asp?n=%d'


class PAD(commands.Cog):
    """Displays PAD monster data"""

    def __init__(self, bot):
        self.bot = bot
        self.use_emotes = False
        self.updating = False
        self.load_emotes.start()  # pylint: disable=no-member
        self.update_db.start()  # pylint: disable=no-member
        self.max_mons = 6500
        self._update_max_mons()

    def cog_unload(self):
        self.load_emotes.cancel()  # pylint: disable=no-member
        self.update_db.cancel()  # pylint: disable=no-member

    def _update_max_mons(self):
        jp_db = UTILS.DB(PAD_DATA.CARDS_DB % JP)
        if jp_db.table_exists('monsters'):
            try:
                self.max_mons = jp_db.execute_one('SELECT MAX(id) AS max_id FROM monsters')
            except:
                pass

    def _get_rdm_mons(self):
        m = None
        while not m:
            m1 = PAD_DATA.get_monster(random.randint(1, self.max_mons), NA)
            m2 = PAD_DATA.get_monster(random.randint(1, self.max_mons), JP)
            m = m1 or m2
        return m

    """
    LISTENERS/TASKS
    """

    @tasks.loop(hours=24)
    async def update_db(self):
        if not self.updating:
            self.updating = True
            LOGGER.debug('Updating PAD DB.')
            await self._update_db()
            LOGGER.debug('Updated PAD DB.')
            self.updating = False

    @update_db.before_loop
    async def before_update_db(self):
        await self.bot.wait_until_ready()
        now = datetime.now()
        # 8am eastern time ish
        update_time = datetime(now.year, now.month, now.day, 12, 0)
        await discord.utils.sleep_until(update_time)

    @tasks.loop(count=1)
    async def load_emotes(self):
        if 'emotes' in self.bot.config:
            AwakeningEmoji.load_emojis(self, self.bot, self.bot.config['emotes'])

    @load_emotes.before_loop
    async def before_load_emotes(self):
        await self.bot.wait_until_ready()

    @commands.Cog.listener()
    async def on_reaction_add(self, reaction, user):
        if user == self.bot.user or not reaction.message.embeds:
            return
        msg = reaction.message
        embed = msg.embeds[0]
        if not embed.title:
            return
        if not self.bot.user in [u async for u in reaction.users()]:
            return
        e = reaction.emoji
        title = embed.title
        if e == X:
            await msg.delete()
        elif title.startswith('Series: '):
            footer = embed.footer.text
            region = footer.split(' ')[0].lower()
            curr_page = int(footer.split(' ')[1].split('/')[0])
            max_page = int(footer.split(' ')[1].split('/')[1])
            series_query = title.replace('Series: ', '', 1)
            new_page = curr_page
            if e == LEFT_TRIANGLE:
                new_page -= 1
            elif e == RIGHT_TRIANGLE:
                new_page += 1
            if new_page != curr_page and 1 <= new_page <= max_page:
                await msg.edit(embed=self._series_embed(series_query, region, new_page))
            await msg.remove_reaction(reaction, user)
        elif title.startswith('No.'):
            all_reactions = [r.emoji for r in filter(lambda r:
                                                     (r.emoji == REGIONAL_INDICATOR_N
                                                      or r.emoji == REGIONAL_INDICATOR_J)
                                                     and r.me,
                                                     msg.reactions)]
            region = NA if REGIONAL_INDICATOR_J in all_reactions else JP
            embed_data = UTILS.get_embed_data(embed)
            region = NA if embed_data['region'] == NA else JP
            num = int(title.replace('No.', '').split(' ')[0])
            m = PAD_DATA.get_monster(num, region)
            new_num = num
            if e == LEFT_ARROW:
                for n in reversed(m.evolutions):
                    if n < num:
                        new_num = n
                        break
            elif e == RIGHT_ARROW:
                for n in m.evolutions:
                    if n > num:
                        new_num = n
                        break
            elif e == REGIONAL_INDICATOR_J or e == REGIONAL_INDICATOR_N:
                new_region = self._flip_region(region)
                new_embed = self._info_embed(user, str(num), new_region)
                if new_embed:
                    await msg.edit(embed=new_embed)
                    await msg.clear_reactions()
                    await self._add_info_reactions(msg, new_region)
            if new_num != num:
                await msg.edit(embed=self._info_embed(user, str(new_num), region))
            await msg.remove_reaction(reaction, user)

    def _flip_region(self, region):
        if region == NA:
            return JP
        else:
            return NA

    """
    COMMANDS
    """

    async def _update_db(self):
        await asyncio.get_event_loop().run_in_executor(ThreadPoolExecutor(), PAD_DATA.update_monsters)
        self._update_max_mons()

    @commands.command(description='Update the internal PAD DB data',
                      help='Takes roughly 2 minutes',
                      brief='Update DB')
    async def update(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not self.updating:
            self.updating = True
            await ctx.send('Updating DB. Might take a while.')
            await self._update_db()
            await ctx.send('Updated DB.')
            self.updating = False
        else:
            await ctx.send('Already updating.')

    async def _info(self, ctx, query, region):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not query:
            query = str(self._get_rdm_mons().id)
        e = self._info_embed(ctx.author, query, region)
        if not e:
            region = self._flip_region(region)
            e = self._info_embed(ctx.author, query, region)
        if e:
            return await self._add_info_reactions(await ctx.send(embed=e), region)
        await ctx.send('Nothing was found.')

    @commands.command(aliases=['i'],
                      description='Show info for monster from *query*',
                      help='Use reactions to do different actions\nLeft/Right: Go through evos\nN/J: Switch between NA and JP\nX: Delete embed',
                      brief='Show info')
    async def info(self, ctx, *, query=None):
        await self._info(ctx, query, NA)

    @commands.command(aliases=['ij'],
                      description='Show info for JP monster from *query*',
                      help='Use reactions to do different actions\nLeft/Right: Go through evos\nN/J: Switch between NA and JP\nX: Delete embed',
                      brief='Show JP info')
    async def infojp(self, ctx, *, query=None):
        await self._info(ctx, query, JP)

    async def _pic(self, ctx, query, region):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not query:
            query = str(self._get_rdm_mons().id)
        elif query == 'sheen':
            roll = random.randint(1, 100)
            if roll >= 95:
                return await ctx.send('http://i.imgur.com/oicGMFu.png')
        e = self._pic_embed(query, region)
        if not e:
            new_region = self._flip_region(region)
            e = self._pic_embed(query, new_region)
        if e:
            await ctx.send(embed=e)
        else:
            await ctx.send('Nothing was found.')

    @commands.command(aliases=['p'],
                      description='Show pictures for monster from *query*',
                      help='If animated, will also display MP4 and GIF(JIF) links',
                      brief='Show picture')
    async def pic(self, ctx, *, query=None):
        await self._pic(ctx, query, NA)

    @commands.command(aliases=['pj'],
                      description='Show pictures for JP monster from *query*',
                      help='If animated, will also display MP4 and GIF(JIF) links',
                      brief='Show JP picture')
    async def picjp(self, ctx, *, query=None):
        await self._pic(ctx, query, JP)

    async def _series(self, ctx, query, region):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        e = self._series_embed(query, region)
        if e:
            await self._add_series_reactions(await ctx.send(embed=e))
        else:
            await ctx.send('Invalid series.')

    @commands.command(description='Show monsters with *query* in series name',
                      help='Use reactions to do different actions:\nLeft/Right: Change page\nX: Delete embed',
                      brief='Show series')
    async def series(self, ctx, *, query):
        await self._series(ctx, query, JP)

    """
    EMBEDS
    """

    async def _add_info_reactions(self, message, region):
        await message.add_reaction(LEFT_ARROW)
        await message.add_reaction(RIGHT_ARROW)
        await message.add_reaction(REGIONAL_INDICATOR_J if region == NA else REGIONAL_INDICATOR_N)
        await message.add_reaction(X)

    async def _add_series_reactions(self, message):
        await message.add_reaction(LEFT_TRIANGLE)
        await message.add_reaction(RIGHT_TRIANGLE)
        await message.add_reaction(X)

    def _info_embed(self, author, query, region):
        m = PAD_DATA.get_monster(query, region)
        if not m:
            return None
        active = m.active
        leader = m.leader
        evolutions = m.evolutions
        awakenings = m.awakenings
        supers = m.supers
        latent_slots = m.latent_slots

        att1 = Attribute.from_id(m.attribute_1_id)
        embed_colour = Colour.from_rgb(255, 255, 254)
        if att1 == Attribute.FIRE:
            embed_colour = Colour.from_rgb(255, 116, 75)
        elif att1 == Attribute.WATER:
            embed_colour = Colour.from_rgb(64, 255, 255)
        elif att1 == Attribute.WOOD:
            embed_colour = Colour.from_rgb(76, 217, 98)
        elif att1 == Attribute.LIGHT:
            embed_colour = Colour.from_rgb(242, 231, 76)
        elif att1 == Attribute.DARK:
            embed_colour = Colour.from_rgb(204, 84, 194)

        types = list(filter(lambda t: t != Type.NONE, [Type.from_id(m.type_1_id),
                                                       Type.from_id(m.type_2_id),
                                                       Type.from_id(m.type_3_id)]))
        desc = '/'.join(map(str, types)) + '\n'

        if awakenings and self.use_emotes:
            desc += ''.join([str(e.to_emoji()) for e in map(AwakeningEmoji.from_id, awakenings)])
        elif awakenings:
            desc += ' '.join([Awakening.from_id(a).short_name() for a in awakenings])
        else:
            desc += 'No Awakenings.'

        if supers and self.use_emotes:
            desc += '\n' + ''.join([str(e.to_emoji()) for e in map(AwakeningEmoji.from_id, supers)])
        elif supers:
            desc += '\n' + ' '.join([Awakening.from_id(a).short_name() for a in supers])

        valid_killers = [l.name() for l in m.get_valid_killer_latents()]
        if valid_killers:
            desc += f'\n({latent_slots} slots): '
            if len(valid_killers) == 8:
                desc += 'Any'
            else:
                desc += ' '.join(map(str, valid_killers))

        m_info = '```'
        m_info += '{:11} {}\n'.format('Rarity', m.rarity)
        m_info += '{:11} {}\n'.format('Cost', m.cost)
        m_info += '{:11} {}\n'.format('MP', m.mp)
        m_info += '{:11} {}'.format('Inheritable', 'Yes' if m.inheritable else 'No')
        m_info += '```'

        stats = '```'
        stats += '{:3} {:>5}'.format('Lv', m.max_level)
        stats += '|{:5}|{:5}\n'.format(110, 120) if m.lb_mult else '\n'

        stats += '{:3} {:5}'.format('HP', m.hp_max)
        stats += '|{:5}|{:5}\n'.format(m.lb_hp(), m.super_lb_hp()) if m.lb_mult else '\n'

        stats += '{:3} {:5}'.format('ATK', m.atk_max)
        stats += '|{:5}|{:5}\n'.format(m.lb_atk(), m.super_lb_atk()) if m.lb_mult else '\n'

        stats += '{:3} {:5}'.format('RCV', m.rcv_max)
        stats += '|{:5}|{:5}\n'.format(m.lb_rcv(), m.super_lb_rcv()) if m.lb_mult else '\n'

        stats += '{:3} {:5}'.format('WHT', m.weighted())
        stats += '|{:5}|{:5}\n'.format(m.lb_weighted(), m.super_lb_weighted()) if m.lb_mult else '\n'

        stats += '{:3} {:5}'.format('XP', m.exp)
        stats += '```'

        similar = [mons.id for mons in PAD_DATA.get_monsters(query, region)]
        similar = [n for n in filter(lambda n: not n in evolutions, similar)]
        similar.remove(m.id)

        e = Embed()
        e.title = f'No.{m.id} {m.name}'
        e.url = PDX_LINK % m.id
        e.description = f'**{desc}**'
        e.colour = embed_colour

        e.set_thumbnail(url=PAD_DATA.get_portrait_url(str(m.id), region))
        e.add_field(name='Info', value=m_info, inline=True)
        e.add_field(name='Stats', value=stats, inline=True)
        if active:
            e.add_field(name='Active: ' + active.name + f' ({active.turn_max}->{ active.turn_min})',
                        value=active.desc,
                        inline=False)
        if leader:
            ls_hp = str(floor(leader.max_hp ** 2 * 1000) / 1000.0).rstrip('0').rstrip('.')
            ls_atk = str(floor(leader.max_atk ** 2 * 1000) / 1000.0).rstrip('0').rstrip('.')
            ls_rcv = str(floor(leader.max_rcv ** 2 * 1000) / 1000.0).rstrip('0').rstrip('.')
            ls_shield = str(floor((100 * (1 - ((1 - leader.max_shield) ** 2))) * 1000) / 1000.0).rstrip('0').rstrip('.')
            shield = '/{}'.format(ls_shield) if leader.max_shield > 0 else ''
            e.add_field(name=f'Leader: {leader.name} [{ls_hp}/{ls_atk}/{ls_rcv}{shield}]',
                        value=leader.desc,
                        inline=False)
        if evolutions:
            e.add_field(name='Other Evos', value=', '.join([f'[{e}]({PDX_LINK % e})' for e in evolutions]), inline=True)
        if similar:
            e.add_field(name='Similar Names',
                        value=', '.join([f'[{s}]({PDX_LINK % s})' for s in similar]) if len(similar) <= 10 else 'Too many to show.',
                        inline=True)
        data = {
            'query': query,
            'region': region
        }
        e.set_footer(text=f'Series: {m.series}', icon_url=UTILS.make_data_url(author, data))
        return e

    def _series_embed(self, query, region, page=1):
        query = query.lower()
        monsters = PAD_DATA.get_series(query, region)
        if not monsters:
            return None
        per_page = 10
        mons_page = monsters[per_page * (page - 1):per_page * (page - 1) + per_page]
        e = Embed(
            title=f'Series: {query}',
            description='\n'.join([f'**{m.id}**: {m.name}' for m in mons_page])
        ).set_footer(text=f'{region.upper()} {page}/{ceil(len(monsters)/per_page)}')
        return e

    def _pic_embed(self, query, region):
        m = PAD_DATA.get_monster(query, region)
        url = PAD_DATA.get_picture_url(m, region)
        if m and url:
            desc = ''
            if m.is_animated:
                mp4 = PAD_DATA.get_animated_mp4_url(m, region)
                gif = PAD_DATA.get_animated_gif_url(m, region)
                desc = (f'[MP4]({mp4})' if mp4 else '') + (f' | [GIF]({gif})' if gif else '')
            return Embed(title=f'No.{m.id} {m.name}',
                         description=desc).set_image(url=url)
        return None


awakening_emojis = {}


class AwakeningEmoji(Enum):
    UNKNOWN = (0, "Unknown")
    HP = (1, "HP")
    ATK = (2, "ATK")
    RCV = (3, "RCV")
    FIRERES = (4, "FireRes")
    WATERRES = (5, "WaterRes")
    WOODRES = (6, "WoodRes")
    LIGHTRES = (7, "LightRes")
    DARKRES = (8, "DarkRes")
    AUTORCV = (9, "AutoRCV")
    BINDRES = (10, "BindRes")
    BLINDRES = (11, "BlindRes")
    JAMRES = (12, "JamRes")
    POIRES = (13, "PoiRes")
    FIREOE = (14, "FireOE")
    WATEROE = (15, "WaterOE")
    WOODOE = (16, "WoodOE")
    LIGHTOE = (17, "LightOE")
    DARKOE = (18, "DarkOE")
    HEARTOE = (29, "HeartOE")
    TIMEEXTEND = (19, "TE")
    BINDRCV = (20, "BindRCV")
    SKILLBOOST = (21, "SB")
    TPA = (27, "TPA")
    SBR = (28, "SBR")
    FIREROW = (22, "FireRow")
    WATERROW = (23, "WaterRow")
    WOODROW = (24, "WoodRow")
    LIGHTROW = (25, "LightRow")
    DARKROW = (26, "DarkRow")
    COOP = (30, "CoopBoost")
    DRAGONKILLER = (31, "DragonKill")
    GODKILLER = (32, "GodKill")
    DEVILKILLER = (33, "DevilKill")
    MACHINEKILLER = (34, "MachineKill")
    BALANCEDKILLER = (35, "BalanceKill")
    ATTACKERKILLER = (36, "AttackerKill")
    PHYSICALKILLER = (37, "PhysKill")
    HEALERKILLER = (38, "HealerKill")
    EVOMATKILLER = (39, "EvoKill")
    AWOKENMATKILLER = (40, "AwokenKill")
    ENHANCEKILLER = (41, "EnhanceKill")
    VENDORKILLER = (42, "RedeemKill")
    SEVENC = (43, "7c")
    GUARDBREAK = (44, "GuardBreak")
    FOLLOWUPATTACK = (45, "FUA")
    TEAMHP = (46, "TeamHP")
    TEAMRCV = (47, "TeamRCV")
    VOIDPEN = (48, "VoidPen")
    ASSIST = (49, "Assist")
    SFUA = (50, "SFUA")
    CHARGE = (51, "Charge")
    BINDRESPLUS = (52, "BindResPlus")
    TEPLUS = (53, "TEPlus")
    CLOUDRES = (54, "CloudRes")
    RIBBONRES = (55, "RibbonRes")
    SBPLUS = (56, "SBPlus")
    HP80BOOST = (57, "80Boost")
    HP50BOOST = (58, "50Boost")
    LSHIELD = (59, "LShield")
    LUNLOCK = (60, "LUnlock")
    TENC = (61, "10c")

    COMBOORB = (62, "ComboOrb")
    VOICE = (63, "Voice")
    DUNGEONBOOST = (64, "DungeonBoost")

    MINUSHP = (65, "MinusHP")
    MINUSATK = (66, "MinusATK")
    MINUSRCV = (67, "MinusRCV")

    BLINDRESPLUS = (68, "BlindResPlus")
    JAMMERRESPLUS = (69, "JammerResPlus")
    POISONRESPLUS = (70, "PoisonResPlus")

    JAMMERBLESSING = (71, "JammerBlessing")
    POISONBLESSING = (72, "PoisonBlessing")

    FIRECOMBO = (73, "FireCombo")
    WATERCOMBO = (74, "WaterCombo")
    WOODCOMBO = (75, "WoodCombo")
    LIGHTCOMBO = (76, "LightCombo")
    DARKCOMBO = (77, "DarkCombo")
    CROSSATTACK = (78, "CrossAttack")

    def id(self):
        return self.value[0]  # pylint: disable=unsubscriptable-object

    def emote(self):
        return self.value[1]  # pylint: disable=unsubscriptable-object

    def to_emoji(self):
        return awakening_emojis[self.id()]

    def __str__(self):
        return self.emote()

    @classmethod
    def from_str(cls, s):
        for ae in cls:
            if s == ae.emote():
                return ae
        return AwakeningEmoji.UNKNOWN

    @classmethod
    def from_id(cls, i):
        for ae in cls:
            if i == ae.id():
                return ae
        return AwakeningEmoji.UNKNOWN

    @classmethod
    def load_emojis(cls, pad_cog, bot, servers):
        emojis_to_find = [e.emote() for e in AwakeningEmoji]
        for s in servers:
            g = bot.get_guild(s)
            if g:
                for g_e in g.emojis:
                    if g_e.name in emojis_to_find:
                        awakening_emojis[AwakeningEmoji.from_str(
                            g_e.name).id()] = g_e
        if len(awakening_emojis) < len(emojis_to_find):
            LOGGER.error('Not all awakening emojis were loaded.')
            pad_cog.use_emotes = False
        else:
            pad_cog.use_emotes = True


def setup(bot):
    bot.add_cog(PAD(bot))
