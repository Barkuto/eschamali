from discord.ext import commands


class Perms(commands.Cog):
    """Handles enabled/disabled cogs, and channels for specific cogs"""

    def __init__(self, bot):
        self.CHANNELS_TABLE = 'cog_channels'
        self.CHANNELS_TABLE_COL1 = ('cog', bot.db.TEXT)
        self.CHANNELS_TABLE_COL2 = ('channels', bot.db.TEXT)

        self.COGS_TABLE = 'cogs'
        self.COGS_TABLE_COL1 = ('cog', bot.db.TEXT)
        self.COGS_TABLE_COL2 = ('enabled', bot.db.INTEGER)

        self.SELF_COG_NAME = 'perms'

        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = self.bot.utils.get_server_db(guild)
        db.create_table(self.CHANNELS_TABLE, self.CHANNELS_TABLE_COL1, self.CHANNELS_TABLE_COL2)
        db.create_table(self.COGS_TABLE, self.COGS_TABLE_COL1, self.COGS_TABLE_COL2)
        for cog in self.bot.all_cogs():
            if not db.get_row(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL1[0], cog)):
                db.insert_row(self.CHANNELS_TABLE,
                              (self.CHANNELS_TABLE_COL1[0], cog),
                              (self.CHANNELS_TABLE_COL2[0], 'all'))
            if not db.get_row(self.COGS_TABLE, (self.COGS_TABLE_COL1[0], cog)):
                db.insert_row(self.COGS_TABLE,
                              (self.COGS_TABLE_COL1[0], cog),
                              (self.COGS_TABLE_COL2[0], 1))

    @commands.Cog.listener()
    async def on_guild_channel_delete(self, channel):
        db = self.bot.utils.get_server_db(channel)
        db.delete_rows(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL2[0], channel.id))
        self.bot.vars.LOGGER.info('[%d] Removed deleted channel %s from db.' % (channel.guild.id, channel.id))
        self._init_db(channel.guild)

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    """
    SERVER COG PERMS
    """

    @commands.command(aliases=['sc'], ignore_extra=False,
                      description='Show server cogs that are enabled or disabled',
                      help='Requires **Manage Guild** permission\n:o: = Enabled\n:x: = Disabled',
                      brief='Server cogs')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def server_cogs(self, ctx):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        db = self.bot.utils.get_server_db(ctx)
        result = sorted(db.get_all(self.COGS_TABLE))
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
            db = self.bot.utils.get_server_db(guild)
            if db.get_row(self.COGS_TABLE, (self.COGS_TABLE_COL1[0], cog_name)):
                db.delete_rows(self.COGS_TABLE,
                               (self.COGS_TABLE_COL1[0], cog_name))
                db.insert_row(self.COGS_TABLE,
                              (self.COGS_TABLE_COL1[0], cog_name),
                              (self.COGS_TABLE_COL2[0], enable))
                return True

    @commands.command(aliases=['ec'],
                      description='Enable a server cog',
                      help='Requires **Manage Guild** permission',
                      brief='Enable cog')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def enable_cog(self, ctx, *, cog_name):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel) and not cog_name.lower() == self.SELF_COG_NAME:
            return
        if self._enable_cog(ctx.guild, cog_name, 1):
            await ctx.send(f'Enabled `{cog_name.lower()}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dc'],
                      description='Disable a server cog',
                      help='Requires **Manage Guild** permission',
                      brief='Dsiable cog')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def disable_cog(self, ctx, *, cog_name):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        if self._enable_cog(ctx.guild, cog_name, 0):
            await ctx.send(f'Disabled `{cog_name.lower()}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dac'],
                      description='Disable all server cogs',
                      help='Requires **Manage Guild** permission',
                      brief='Disable all cogs')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def disable_all_cogs(self, ctx):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        for cog in self.bot.all_cogs():
            self._enable_cog(ctx.guild, cog, 0)
        await ctx.send('Disabled all cogs.')

    @commands.command(aliases=['eac'],
                      description='Enable all server cogs',
                      help='Requires **Manage Guild** permission',
                      brief='Enable all cogs')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def enable_all_cogs(self, ctx):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        for cog in self.bot.all_cogs():
            self._enable_cog(ctx.guild, cog, 1)
        await ctx.send('Enabled all cogs.')

    """
    COG CHANNEL PERMS
    """

    @commands.command(aliases=['acch'],
                      description='Add a channel to a cog',
                      help='Requires **Manage Guild** permission',
                      brief='Add cog channel')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def add_cog_channel(self, ctx, cog_name, *channels):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            mentions = ctx.message.channel_mentions
            db = self.bot.utils.get_server_db(ctx)
            if mentions:
                db.delete_rows(self.CHANNELS_TABLE,
                               (self.CHANNELS_TABLE_COL1[0], cog_name),
                               (self.CHANNELS_TABLE_COL2[0], 'all'))
            for c in mentions:
                if not db.get_row(self.CHANNELS_TABLE,
                                  (self.CHANNELS_TABLE_COL1[0], cog_name),
                                  (self.CHANNELS_TABLE_COL2[0], c.id)):
                    db.insert_row(self.CHANNELS_TABLE,
                                  (self.CHANNELS_TABLE_COL1[0], cog_name),
                                  (self.CHANNELS_TABLE_COL2[0], c.id))
            added = ' '.join([c.mention for c in mentions])
            await ctx.send(f'Added {added} to `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['dcch'],
                      description='Delete a channel from a cog',
                      help='Requires **Manage Guild** permission',
                      brief='Delete cog channel')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def delete_cog_channel(self, ctx, cog_name, *channels):
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            mentions = ctx.message.channel_mentions
            db = self.bot.utils.get_server_db(ctx)
            for c in mentions:
                db.delete_rows(self.CHANNELS_TABLE,
                               (self.CHANNELS_TABLE_COL1[0], cog_name),
                               (self.CHANNELS_TABLE_COL2[0], c.id))

            if not db.get_row(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL1[0], cog_name)):
                await self.reset_cog_channels(ctx, cog_name=cog_name)

            deleted = ' '.join([c.mention for c in mentions])
            await ctx.send(f'Deleted {deleted} from `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['rcch'],
                      description='Reset a cogs channels',
                      help='Requires **Manage Guild** permission\nSets the cog channels back to "all"',
                      brief='Reset cog channels')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def reset_cog_channels(self, ctx, *, cog_name):
        # Allow reset anywhere?
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            db = self.bot.utils.get_server_db(ctx)
            db.delete_rows(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL1[0], cog_name))
            db.insert_row(self.CHANNELS_TABLE,
                          (self.CHANNELS_TABLE_COL1[0], cog_name),
                          (self.CHANNELS_TABLE_COL2[0], 'all'))
            await ctx.send(f'Reset all channels for `{cog_name}`')
        else:
            await ctx.send('Invalid cog.')

    @commands.command(aliases=['cch'],
                      description='Show channels for a cog in the server',
                      help='Requires **Manage Guild** permission',
                      brief='Show cog channels')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def server_cog_channels(self, ctx, *, cog_name):
        # Allow see anywhere?
        if not self.can_cog_in(self.SELF_COG_NAME, ctx.channel):
            return
        cog_name = cog_name.lower()
        if self._is_cog(cog_name) and self._is_loaded_cog(cog_name):
            db = self.bot.utils.get_server_db(ctx)
            result = db.get_rows(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL1[0], cog_name))
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
        db = self.bot.utils.get_server_db(guild)
        return True if db.get_value(self.COGS_TABLE, self.COGS_TABLE_COL2[0], (self.COGS_TABLE_COL1[0], cog_name)) else False

    def can_cog_in(self, cog_name, channel):
        if not self.is_cog_enabled(channel.guild, cog_name):
            return False
        cog_name = cog_name.lower()
        db = self.bot.utils.get_server_db(channel)
        result = db.get_rows(self.CHANNELS_TABLE, (self.CHANNELS_TABLE_COL1[0], cog_name))
        channels = [r[1] for r in result]
        for c in channels:
            if c == 'all' or int(c) == channel.id:
                return True
        return False


def setup(bot):
    bot.add_cog(Perms(bot))
