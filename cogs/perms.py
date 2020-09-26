import importlib
import discord
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = UTILS.DB
LOGGER = UTILS.VARS.LOGGER

CHANNELS_TABLE = 'cog_channels'
CHANNELS_TABLE_COL1 = ('cog', DB_MOD.TEXT)
CHANNELS_TABLE_COL2 = ('channels', DB_MOD.TEXT)

COGS_TABLE = 'cogs'
COGS_TABLE_COL1 = ('cog', DB_MOD.TEXT)
COGS_TABLE_COL2 = ('enabled', DB_MOD.INTEGER)

SELF_COG_NAME = 'perms'


class Perms(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = UTILS.get_server_db(guild)
        db.create_table(CHANNELS_TABLE, CHANNELS_TABLE_COL1, CHANNELS_TABLE_COL2)
        db.create_table(COGS_TABLE, COGS_TABLE_COL1, COGS_TABLE_COL2)
        for cog in self.bot.all_cogs():
            if not db.get_row(CHANNELS_TABLE, (CHANNELS_TABLE_COL1[0], cog)):
                db.insert_row(CHANNELS_TABLE,
                              (CHANNELS_TABLE_COL1[0], cog),
                              (CHANNELS_TABLE_COL2[0], 'all'))
            if not db.get_row(COGS_TABLE, (COGS_TABLE_COL1[0], cog)):
                db.insert_row(COGS_TABLE,
                              (COGS_TABLE_COL1[0], cog),
                              (COGS_TABLE_COL2[0], 1))

    @commands.Cog.listener()
    async def on_guild_channel_delete(self, channel):
        db = UTILS.get_server_db(channel)
        db.delete_rows(CHANNELS_TABLE, (CHANNELS_TABLE_COL2[0], channel.id))
        LOGGER.info('[%d] Removed deleted channel %s from db.' % (channel.guild.id, channel.id))
        self._init_db(channel.guild)

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    """
    SERVER COG PERMS
    """

    @commands.command(aliases=['sc'], ignore_extra=False)
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def server_cogs(self, ctx):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        result = sorted(db.get_all(COGS_TABLE))
        m = ''
        for r in result:
            if r[0] in self.bot.loaded_cogs():
                s = ':o:' if r[1] else ':x:'
                m += f'{s} `{r[0]}`\n'
        if m:
            await ctx.send(m)

    def _enable_cog(self, guild, cog_name, enable):
        cog_name = cog_name.lower()
        if not self._is_cog(cog_name) or not self._is_loaded_cog(cog_name):
            return False
        else:
            db = UTILS.get_server_db(guild)
            if db.get_row(COGS_TABLE, (COGS_TABLE_COL1[0], cog_name)):
                db.delete_rows(COGS_TABLE,
                               (COGS_TABLE_COL1[0], cog_name))
                db.insert_row(COGS_TABLE,
                              (COGS_TABLE_COL1[0], cog_name),
                              (COGS_TABLE_COL2[0], enable))
                return True

    @commands.command(aliases=['ec'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def enable_cog(self, ctx, *, cog_name):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel) and not cog_name.lower() == SELF_COG_NAME:
            return
        if self._enable_cog(ctx.guild, cog_name, 1):
            await ctx.send(f'Enabled `{cog_name.lower()}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dc'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def disable_cog(self, ctx, *, cog_name):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        if self._enable_cog(ctx.guild, cog_name, 0):
            await ctx.send(f'Disabled `{cog_name.lower()}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dac'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def disable_all_cogs(self, ctx):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        for cog in self.bot.all_cogs():
            self._enable_cog(ctx.guild, cog, 0)
        await ctx.send('Disabled all cogs.')

    @commands.command(aliases=['eac'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def enable_all_cogs(self, ctx):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        for cog in self.bot.all_cogs():
            self._enable_cog(ctx.guild, cog, 1)
        await ctx.send('Enabled all cogs.')

    """
    COG CHANNEL PERMS
    """

    @commands.command(aliases=['acch'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def add_cog_channel(self, ctx, cog_name, *channels):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            mentions = ctx.message.channel_mentions
            db = UTILS.get_server_db(ctx)
            if mentions:
                db.delete_rows(CHANNELS_TABLE,
                               (CHANNELS_TABLE_COL1[0], cog_name),
                               (CHANNELS_TABLE_COL2[0], 'all'))
            for c in mentions:
                if not db.get_row(CHANNELS_TABLE,
                                  (CHANNELS_TABLE_COL1[0], cog_name),
                                  (CHANNELS_TABLE_COL2[0], c.id)):
                    db.insert_row(CHANNELS_TABLE,
                                  (CHANNELS_TABLE_COL1[0], cog_name),
                                  (CHANNELS_TABLE_COL2[0], c.id))
            added = ' '.join([c.mention for c in mentions])
            await ctx.send(f'Added {added} to `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dcch'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def delete_cog_channel(self, ctx, cog_name, *channels):
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            mentions = ctx.message.channel_mentions
            db = UTILS.get_server_db(ctx)
            for c in mentions:
                db.delete_rows(CHANNELS_TABLE,
                               (CHANNELS_TABLE_COL1[0], cog_name),
                               (CHANNELS_TABLE_COL2[0], c.id))

            if not db.get_row(CHANNELS_TABLE, (CHANNELS_TABLE_COL1[0], cog_name)):
                await self.reset_cog_channels(ctx, cog_name=cog_name)

            deleted = ' '.join([c.mention for c in mentions])
            await ctx.send(f'Deleted {deleted} from `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['rcch'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def reset_cog_channels(self, ctx, *, cog_name):
        # Allow reset anywhere?
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            db = UTILS.get_server_db(ctx)
            db.delete_rows(CHANNELS_TABLE, (CHANNELS_TABLE_COL1[0], cog_name))
            db.insert_row(CHANNELS_TABLE,
                          (CHANNELS_TABLE_COL1[0], cog_name),
                          (CHANNELS_TABLE_COL2[0], 'all'))
            await ctx.send(f'Reset all channels for `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['cch'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def server_cog_channels(self, ctx, *, cog_name):
        # Allow see anywhere?
        if not self.can_cog_in(SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            db = UTILS.get_server_db(ctx)
            result = db.get_rows(CHANNELS_TABLE, (CHANNELS_TABLE_COL1[0], cog_name))
            channels = [r[1] if r[1] == 'all'
                        else ctx.guild.get_channel(int(r[1]))
                        for r in result]
            channels = [c if c == 'all'
                        else c.mention
                        for c in channels]
            await ctx.send('`%s`: %s' % (cog_name, ' '.join(channels)))
        else:
            await ctx.send('Invalid cog.')

    """
    CHECKERS
    """

    def _is_cog(self, cog_name):
        return cog_name in self.bot.all_cogs()

    def _is_loaded_cog(self, cog_name):
        return cog_name in self.bot.loaded_cogs()

    def is_cog_enabled(self, guild, cog_name):
        cog_name = cog_name.lower()
        db = UTILS.get_server_db(guild)
        return True if db.get_value(COGS_TABLE, COGS_TABLE_COL2[0], (COGS_TABLE_COL1[0], cog_name)) else False

    def can_cog_in(self, cog_name, channel):
        if not self.is_cog_enabled(channel.guild, cog_name):
            return False
        cog_name = cog_name.lower()
        db = UTILS.get_server_db(channel)
        result = db.get_rows(CHANNELS_TABLE, (CHANNELS_TABLE_COL1[0], cog_name))
        channels = [r[1] for r in result]
        for c in channels:
            if c == 'all' or int(c) == channel.id:
                return True
        return False


def setup(bot):
    bot.add_cog(Perms(bot))
