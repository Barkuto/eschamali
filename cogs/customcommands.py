import importlib
from discord import Embed, DMChannel
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
VARS = UTILS.VARS
DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB

CUSTOM_TABLE = 'custom_commands'
CUSTOM_TABLE_COL1 = ('command', DB_MOD.TEXT)
CUSTOM_TABLE_COL2 = ('message', DB_MOD.TEXT)

PREFIX = '!'


class CustomCommands(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = UTILS.get_server_db(guild)
        db.create_table(CUSTOM_TABLE, CUSTOM_TABLE_COL1,  CUSTOM_TABLE_COL2)

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        ctx = await self.bot.get_context(msg)
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        m = msg.content
        split = m.split(' ')
        if m.startswith(PREFIX) and len(split) == 1:
            db = UTILS.get_server_db(msg.guild)
            cmds = db.get_all(CUSTOM_TABLE)
            cmd = split[0].split(PREFIX)[1].lower()
            for c_name, c_text in cmds:
                if c_name == cmd:
                    return await ctx.send(c_text)

    @commands.command(aliases=['acc'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def addcustomcommand(self, ctx, name, *, text):
        if not UTILS.can_cog_in(self, ctx):
            return
        name = name.lower()
        db = UTILS.get_server_db(ctx)
        check = db.get_value(CUSTOM_TABLE, CUSTOM_TABLE_COL2[0], (CUSTOM_TABLE_COL1[0], name))
        if not check:
            db.insert_row(CUSTOM_TABLE, (CUSTOM_TABLE_COL1[0], name), (CUSTOM_TABLE_COL2[0], text))
            await ctx.send(f'Added `{name}` as a custom command.')
        else:
            db.update_row(CUSTOM_TABLE, (CUSTOM_TABLE_COL1[0], name), (CUSTOM_TABLE_COL2[0], text))
            await ctx.send(f'Edited custom command `{name}`.')

    @commands.command(aliases=['dcc'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def deletecustomcommand(self, ctx, name):
        if not UTILS.can_cog_in(self, ctx):
            return
        name = name.lower()
        db = UTILS.get_server_db(ctx)
        check = db.get_row(CUSTOM_TABLE, (CUSTOM_TABLE_COL1[0], name))
        if check:
            db.delete_rows(CUSTOM_TABLE, (CUSTOM_TABLE_COL1[0], name))
            await ctx.send(f'Deleted `{name}` as a custom command.')
        else:
            await ctx.send(f'Invalid custom command.')

    @commands.command(aliases=['cc'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def customcommands(self, ctx):
        if not UTILS.can_cog_in(self, ctx):
            return
        db = UTILS.get_server_db(ctx)
        cmds = [f'`{c[0]}`' for c in db.get_all(CUSTOM_TABLE)]
        if cmds:
            await ctx.send('Custom Commands !: %s' % ' '.join(cmds))
        else:
            await ctx.send('There are no custom commands.')


def setup(bot):
    bot.add_cog(CustomCommands(bot))
