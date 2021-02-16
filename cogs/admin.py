import re
import os
import os.path
import discord
from discord import Permissions, DMChannel
from discord.ext import commands
from discord.errors import Forbidden
import importlib

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = UTILS.DB
LOGGER = UTILS.VARS.LOGGER

ADMIN_TABLE = 'admin'
ADMIN_TABLE_COL1 = ('field', DB_MOD.TEXT)
ADMIN_TABLE_COL2 = ('role', DB_MOD.TEXT)
BANNED_WORD_FIELD = 'banned_word'
MUTE_ROLE_FIELD = 'mute_role'

STRIKES_TABLE = 'strikes'
STRIKES_TABLE_COL1 = ('user', DB_MOD.INTEGER)
STRIKES_TABLE_COL2 = ('strikes', DB_MOD.INTEGER)


class Admin(commands.Cog):
    """Administrative commands"""

    def __init__(self, bot):
        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = UTILS.get_server_db(guild)
        db.create_table(ADMIN_TABLE, ADMIN_TABLE_COL1,  ADMIN_TABLE_COL2)
        db.create_table(STRIKES_TABLE, STRIKES_TABLE_COL1, STRIKES_TABLE_COL2)

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    """
    USER/MESSAGE FUNCTIONS
    """

    @commands.command(description='Kicks *users* from the server',
                      help='Requires **Kick Members** permission\nUse by **mentioning** users',
                      brief='Kick user(s)')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(kick_members=True))
    async def kick(self, ctx, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        kicked = 0
        not_kicked = []
        for u in ctx.message.mentions:
            try:
                await u.kick()
                kicked += 1
            except Forbidden:
                not_kicked.append(u)
        await ctx.send('Kicked %d people.\n%s' % (
            kicked,
            'Could not kick: ' + ' '.join([u.mention for u in not_kicked])
            if not_kicked else ''))

    @commands.command(description='Bans *users* from the server',
                      help='Requires **Ban Members** permission\nUse by **mentioning** users',
                      brief='Ban user(s)')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(ban_members=True))
    async def ban(self, ctx, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        banned = 0
        not_banned = []
        for u in ctx.message.mentions:
            try:
                await u.ban()
                banned += 1
            except Forbidden:
                not_banned.append(u)
        await ctx.send('Banned %d people.\n%s' % (
            banned,
            'Could not ban: ' + ' '.join([u.mention for u in not_banned])
            if not_banned else ''))

    @commands.command(description='Remove *num* messages of *users*',
                      help='Requires **Manage Messages** permission\nUse by **mentioning** users\nIf no mentions, prune *num* messages in the channel',
                      brief='Remove messages')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True))
    async def prune(self, ctx, num: int, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        total_deleted = 0
        mentions = ctx.message.mentions
        await ctx.message.delete()
        for user in mentions:
            deleted = 0
            async for m in ctx.channel.history(limit=100):
                try:
                    if deleted < num and m.author == user:
                        await m.delete()
                        deleted += 1
                except Exception:
                    pass
            total_deleted += deleted
        if not users:
            async for m in ctx.channel.history(limit=num):
                try:
                    await m.delete()
                    total_deleted += 1
                except Exception:
                    pass
        await ctx.send('Deleted %d messages%s.' % (total_deleted,
                                                   ' from ' + ' '.join([u.mention for u in mentions])
                                                   if users else ''))
    """
    MUTE FUNCTIONS
    """

    @commands.Cog.listener()
    async def on_guild_channel_create(self, channel):
        mute_role = await self.get_create_muterole(channel.guild)
        if mute_role:
            await self.set_channel_mute_perms(mute_role, channel)

    async def get_create_muterole(self, guild):
        db = UTILS.get_server_db(guild)
        role_id = db.get_value(ADMIN_TABLE, ADMIN_TABLE_COL2[0], (ADMIN_TABLE_COL1[0], MUTE_ROLE_FIELD))
        if role_id:
            return guild.get_role(int(role_id))
        else:
            role_name = 'Muted'
            role_colour = discord.Colour.from_rgb(12, 0, 0)
            role = None
            for r in guild.roles:
                if r.name == role_name and r.colour == role_colour:
                    role = r
                    break
            if not role:
                role = await guild.create_role(reason='Create Muted role.',
                                               name=role_name,
                                               colour=role_colour)
                for c in guild.channels:
                    try:
                        await self.set_channel_mute_perms(role, c)
                    except:
                        pass
            self._set_muterole(role)
            return role

    def _set_muterole(self, role):
        db = UTILS.get_server_db(role.guild)
        db.delete_rows(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], MUTE_ROLE_FIELD))
        return db.insert_row(ADMIN_TABLE,
                             (ADMIN_TABLE_COL1[0], MUTE_ROLE_FIELD),
                             (ADMIN_TABLE_COL2[0], role.id))

    async def set_channel_mute_perms(self, role, channel):
        await channel.set_permissions(role, read_messages=False, send_messages=False)

    @commands.command(description='Mute *users*',
                      help='Requires **Manage Roles** permission\nUse by **mentioning** users',
                      brief='Mute user(s)')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_roles=True))
    async def mute(self, ctx, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mute_role = await self.get_create_muterole(ctx.guild)
        if mute_role:
            mentions = ctx.message.mentions
            added = []
            not_added = []
            for u in mentions:
                try:
                    await u.add_roles(mute_role)
                    added.append(u)
                except:
                    not_added.append(u)
            if added:
                m = 'Muted: %s\n' % ' '.join([u.mention for u in added])
            if not_added:
                m = '%sCould not mute: %s' % (m, ' '.join([u.mention for u in not_added]))
            await ctx.send(m)

    @commands.command(description='Unmute *users*',
                      help='Requires **Manage Roles** permission\nUse by **mentioning** users',
                      brief='Unmute user(s)')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_roles=True))
    async def unmute(self, ctx, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mute_role = await self.get_create_muterole(ctx.guild)
        if mute_role:
            mentions = ctx.message.mentions
            removed = []
            not_removed = []
            for u in mentions:
                try:
                    await u.remove_roles(mute_role)
                    removed.append(u)
                except:
                    not_removed.append(u)
            if removed:
                m = 'Unmuted: %s\n' % ' '.join([u.mention for u in removed])
            if not_removed:
                m = '%sCould not unmute: %s' % (m, ' '.join([u.mention for u in not_removed]))
            await ctx.send(m)

    @commands.command(description='Show mute role for current server',
                      help='Requires **Manage Roles** permission',
                      brief='Show mute role')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_roles=True))
    async def muterole(self, ctx, *, role=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.role_mentions
        if role and mentions:
            if self._set_muterole(mentions[0]):
                return await UTILS.confirm(ctx)
        elif role:
            new_role = UTILS.find_role(ctx.guild, role)
            if new_role:
                if self._set_muterole(new_role):
                    return await UTILS.confirm(ctx)
            return await ctx.send('Invalid role.')

        role = await self.get_create_muterole(ctx.guild)
        await ctx.send(embed=discord.Embed(
            title='Mute Role',
            description=role.mention if role else 'None.',
            colour=role.colour if role else 0))

    """
    LOCK FUNCTIONS
    """

    @commands.command(description='Locks current channel',
                      help='Requires **Manage Messages** or **Manage Channels** permission',
                      brief='Lock channel')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True),
                        commands.has_permissions(manage_channels=True))
    async def lock(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if ctx.channel.overwrites[ctx.guild.default_role].send_messages:
            await ctx.send('Channel locked.')
            await ctx.channel.set_permissions(ctx.guild.default_role, send_messages=False)

    @commands.command(description='Unlocks current channel',
                      help='Requires **Manage Messages** or **Manage Channels** permission\nNote that unlocking could overwrite manual channel permissions for @everyone',
                      brief='Unlock channel')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True),
                        commands.has_permissions(manage_channels=True))
    async def unlock(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not ctx.channel.overwrites[ctx.guild.default_role].send_messages:
            await ctx.channel.set_permissions(ctx.guild.default_role, send_messages=True)
            await ctx.send('Channel unlocked.')

    """
    WARNING FUNCTIONS
    """

    @commands.command(description='Warns *users*',
                      help='Requires **Kick Members** or **Ban Members** permission\nYou can specify how many warnings to give before mentioning users\n3 warnings = kick\n5 warnings = ban',
                      brief='Warn user(s)')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(kick_members=True),
                        commands.has_permissions(ban_members=True))
    async def warn(self, ctx, *users):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        num = int(users[0]) if users[0].isdigit() else 1
        if users[0].startswith('-') and users[0][1:].isdigit():
            num = 0 - int(users[0][1:])
        db = UTILS.get_server_db(ctx)
        msg = ''
        for u in ctx.message.mentions:
            strikes = db.get_value(STRIKES_TABLE, STRIKES_TABLE_COL2[0], (STRIKES_TABLE_COL1[0], u.id))
            if not strikes == None:
                strikes = strikes + num
                db.update_row(STRIKES_TABLE, (STRIKES_TABLE_COL1[0], u.id), (STRIKES_TABLE_COL2[0], strikes))
            else:
                strikes = num
                db.insert_row(STRIKES_TABLE, (STRIKES_TABLE_COL1[0], u.id), (STRIKES_TABLE_COL2[0], strikes))
            msg += f'{u.mention} now has {strikes} strikes'
            if strikes >= 5:
                try:
                    await u.ban()
                    msg += ' and has been banned'
                except Forbidden:
                    msg += ' and cannot be banned'
            elif strikes >= 3:
                try:
                    await u.kick()
                    msg += ' and has been kicked'
                except Forbidden:
                    msg += ' and cannot be kicked'
            msg += '.\n'
        if msg:
            await ctx.send(msg)

    @commands.command(description='Check warnings of a given *user*',
                      help='Requires **Kick Members** or **Ban Members** permission\n*user* can be given by mention, name, nickname, or name#discriminator',
                      brief='Show user warnings')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(kick_members=True),
                        commands.has_permissions(ban_members=True))
    async def warnings(self, ctx, *, user):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        u = None
        if ctx.message.mentions:
            u = ctx.message.mentions[0]
        elif user:
            u = UTILS.find_member(ctx.guild, user)
        else:
            return await ctx.send('Invalid user.')
        if u:
            db = UTILS.get_server_db(ctx)
            strikes = db.get_value(STRIKES_TABLE, STRIKES_TABLE_COL2[0], (STRIKES_TABLE_COL1[0], u.id))
            if not strikes == None:
                await ctx.send(f'{u.mention} has {strikes} strike(s).')

    """
    BANNED WORD FUNCTIONS
    """

    async def check_for_banned_words(self, msg):
        words = re.split(' |\n|,', msg.content)
        db = UTILS.get_server_db(msg.guild)
        banned_words = [r[1] for r in db.get_rows(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], BANNED_WORD_FIELD))]
        for w in words:
            for bw in banned_words:
                reg = re.compile(fr'^[_\W]*{bw}[_\W]*$', flags=re.IGNORECASE)
                if re.match(reg, w):
                    return await msg.delete()

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        if not UTILS.can_cog_in(self, msg.channel):
            return
        if msg.author.bot:
            return
        await self.check_for_banned_words(msg)

    @commands.Cog.listener()
    async def on_message_edit(self, before, after):
        if isinstance(before.channel, DMChannel):
            return
        if not UTILS.can_cog_in(self, before.channel):
            return
        await self.check_for_banned_words(after)

    @commands.command(aliases=['abw'],
                      description='Adds new banned *words* to the server',
                      help='Requires **Manage Messages** or **Manage Channels** or **Manage Guild** permission\n*words* are words separated by a space',
                      brief='Adds banned words')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True),
                        commands.has_permissions(manage_channels=True),
                        commands.has_permissions(manage_guild=True))
    async def addbannedwords(self, ctx, *words):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        for w in words:
            w = w.lower()
            if not db.get_row(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], BANNED_WORD_FIELD), (ADMIN_TABLE_COL2[0], w)):
                db.insert_row(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], BANNED_WORD_FIELD), (ADMIN_TABLE_COL2[0], w))
        await ctx.send(f'Added `{len(words)}` banned words.')

    @commands.command(aliases=['dbw'],
                      description='Deletes banned *words* from the server',
                      help='Requires **Manage Messages** or **Manage Channels** or **Manage Guild** permission\n*words* are words separated by a space',
                      brief='Deletes banned words')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True),
                        commands.has_permissions(manage_channels=True),
                        commands.has_permissions(manage_guild=True))
    async def deletebannedwords(self, ctx, *words):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        for w in words:
            db.delete_rows(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], BANNED_WORD_FIELD), (ADMIN_TABLE_COL2[0], w.lower()))
        await ctx.send(f'Deleted `{len(words)}` words.')

    @commands.command(aliases=['bw'],
                      description='Shows banned *words* for the server',
                      help='Requires **Manage Messages** or **Manage Channels** or **Manage Guild** permission',
                      brief='Shows banned words')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_messages=True),
                        commands.has_permissions(manage_channels=True),
                        commands.has_permissions(manage_guild=True))
    async def bannedwords(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        words = [f'`{r[1]}`' for r in db.get_rows(ADMIN_TABLE, (ADMIN_TABLE_COL1[0], BANNED_WORD_FIELD))]
        await ctx.send(embed=discord.Embed(
            title='Banned Words',
            description=' '.join(words)))


def setup(bot):
    bot.add_cog(Admin(bot))
