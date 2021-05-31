from datetime import datetime
from discord import DMChannel
from discord.ext import commands


class CustomCommands(commands.Cog):
    """Server bound custom commands"""

    def __init__(self, bot):
        self.CUSTOM_TABLE = 'custom_commands'
        self.CUSTOM_TABLE_COL1 = ('command', bot.db.TEXT)
        self.CUSTOM_TABLE_COL2 = ('message', bot.db.TEXT)

        self.bot = bot
        self.prefix = bot.pm.cog_prefixes[self.qualified_name]
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = self.bot.utils.get_server_db(guild)
        db.create_table(self.CUSTOM_TABLE, self.CUSTOM_TABLE_COL1, self.CUSTOM_TABLE_COL2)

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        ctx = await self.bot.get_context(msg)
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        if msg.author.bot:
            return
        m = msg.content
        split = m.split(' ')
        if m.startswith(self.prefix) and len(split) == 1:
            db = self.bot.utils.get_server_db(msg.guild)
            cmds = db.get_all(self.CUSTOM_TABLE)
            cmd = split[0].split(self.prefix)[1].lower()
            for c_name, c_text in cmds:
                if c_name == cmd:
                    c_text = c_text.replace('{author}', ctx.author.mention)
                    c_text = c_text.replace('{time}', datetime.now().strftime('%b %d, %Y %I:%M %p'))
                    return await ctx.send(c_text)

    @commands.command(aliases=['acc'],
                      description='Add a custom command to the server',
                      help='Requires **Manage Guild** permission',
                      brief='Add custom command')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def addcustomcommand(self, ctx, name, *, text):
        if not self.bot.utils.can_cog_in(self, ctx):
            return
        name = name.lower()
        db = self.bot.utils.get_server_db(ctx)
        check = db.get_value(self.CUSTOM_TABLE, self.CUSTOM_TABLE_COL2[0], (self.CUSTOM_TABLE_COL1[0], name))
        if not check:
            db.insert_row(self.CUSTOM_TABLE, (self.CUSTOM_TABLE_COL1[0], name), (self.CUSTOM_TABLE_COL2[0], text))
            await ctx.send(f'Added `{name}` as a custom command.')
        else:
            db.update_row(self.CUSTOM_TABLE, (self.CUSTOM_TABLE_COL1[0], name), (self.CUSTOM_TABLE_COL2[0], text))
            await ctx.send(f'Edited custom command `{name}`.')

    @commands.command(aliases=['dcc'],
                      description='Delete a custom command from the server',
                      help='Requires **Manage Guild** permission',
                      brief='Delete custom command')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def deletecustomcommand(self, ctx, name):
        if not self.bot.utils.can_cog_in(self, ctx):
            return
        name = name.lower()
        db = self.bot.utils.get_server_db(ctx)
        check = db.get_row(self.CUSTOM_TABLE, (self.CUSTOM_TABLE_COL1[0], name))
        if check:
            db.delete_rows(self.CUSTOM_TABLE, (self.CUSTOM_TABLE_COL1[0], name))
            await ctx.send(f'Deleted `{name}` as a custom command.')
        else:
            await ctx.send(f'Invalid custom command.')

    @commands.command(aliases=['cc'],
                      description='Show custom commands for ths server',
                      help='All custom commands use the same prefix',
                      brief='Show custom commands')
    async def customcommands(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx):
            return
        db = self.bot.utils.get_server_db(ctx)
        cmds = [f'`{c[0]}`' for c in db.get_all(self.CUSTOM_TABLE)]
        if cmds:
            await ctx.send('Custom Commands !: %s' % ' '.join(cmds))
        else:
            await ctx.send('There are no custom commands.')


def setup(bot):
    bot.add_cog(CustomCommands(bot))
