from discord import Embed
from discord.ext import commands
from discord.errors import Forbidden


class Roles(commands.Cog):
    """Role management commands"""

    def __init__(self, bot):
        self.ROLES_TABLE = 'roles'
        self.ROLES_TABLE_COL1 = ('field', bot.db.TEXT)
        self.ROLES_TABLE_COL2 = ('role', bot.db.INTEGER)

        self.AUTOROLE_FIELD = 'autorole'
        self.SELFROLE_FIELD = 'selfrole'

        self.INVALID_ROLE = 'Invalid role.'

        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = self.bot.utils.get_server_db(guild)
        db.create_table(self.ROLES_TABLE, self.ROLES_TABLE_COL1, self.ROLES_TABLE_COL2)

    """
    AUTOROLE FUNCTIONS
    """

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    def _get_autorole(self, guild):
        db = self.bot.utils.get_server_db(guild)
        role_id = db.get_value(self.ROLES_TABLE, self.ROLES_TABLE_COL2[0], (self.ROLES_TABLE_COL1[0], self.AUTOROLE_FIELD))
        return guild.get_role(role_id)

    def _set_autorole(self, role):
        db = self.bot.utils.get_server_db(role.guild)
        self._remove_autorole(role.guild)
        db.insert_row(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.AUTOROLE_FIELD), (self.ROLES_TABLE_COL2[0], role.id))

    def _remove_autorole(self, guild):
        db = self.bot.utils.get_server_db(guild)
        db.delete_rows(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.AUTOROLE_FIELD))

    @commands.Cog.listener()
    async def on_member_join(self, member):
        if not self.bot.utils.is_cog_enabled(member.guild, self):
            return
        autorole = self._get_autorole(member.guild)
        if autorole:
            await member.add_roles(autorole)

    @commands.command(description='Show or set the autorole',
                      help='Requires **Manage Guild** or **Manage Roles** permission',
                      brief='Autorole')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def autorole(self, ctx, *, role=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.role_mentions
        if role and mentions:
            self._set_autorole(mentions[0])
            return await self.bot.utils.confirm(ctx)
        elif role:
            new_role = self.bot.utils.find_role(ctx.guild, role)
            if new_role:
                self._set_autorole(new_role)
                return await self.bot.utils.confirm(ctx)
            return await ctx.send(self.INVALID_ROLE)

        role = self._get_autorole(ctx.guild)
        await ctx.send(embed=Embed(
            title=f'Autorole for `{ctx.guild}`',
            description=role.mention if role else 'None.',
            colour=role.colour if role else 0))

    @commands.command(aliases=['raa'],
                      description='Remove the autorole',
                      help='Requires **Manage Guild** or **Manage Roles** permission',
                      brief='Remove autorole')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def removeautorole(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        self._remove_autorole(ctx.guild)
        return await self.bot.utils.confirm(ctx)

    """
    BASIC ROLE FUNCTIONS
    """

    @commands.command(aliases=['ar'],
                      description='Add *role* to *user*',
                      help='Requires **Manage Guild** or **Manage Roles** permission',
                      brief='Add role')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def addrole(self, ctx, user, *, role=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.mentions
        if mentions:
            u = mentions[0]
            r = self.bot.utils.find_role(ctx.guild, role)
            if r:
                await u.add_roles(r)
                await self.bot.utils.confirm(ctx)

    @commands.command(aliases=['rr'],
                      description='Remove *role* from *user*',
                      help='Requires **Manage Guild** or **Manage Roles** permission',
                      brief='Remove role')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def removerole(self, ctx, user, *, role=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.mentions
        if mentions:
            u = mentions[0]
            r = self.bot.utils.find_role(ctx.guild, role)
            if r:
                await u.remove_roles(r)
                await self.bot.utils.confirm(ctx)

    @commands.command(description='Check users in *role*',
                      help='*role* can be the role name or id',
                      brief='Check role')
    async def inrole(self, ctx, *, role=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        r = self.bot.utils.find_role(ctx.guild, role)
        if not r:
            return await ctx.send('Invalid role.')
        in_role = [m.mention for m in ctx.guild.members if r in m.roles]
        await ctx.send(embed=Embed(
            title=f'`{r.name}`',
            description=' '.join(in_role),
            colour=r.colour))

    @commands.command(description='Check roles of *user*',
                      help='No arguments gives self roles\n*user* can be mention, name, nickname, name#discriminator, or id',
                      brief='Check role')
    async def roles(self, ctx, *, user=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        roles = []
        title = 'Roles for `%s`'
        mentions = ctx.message.mentions
        if user or mentions:
            u = mentions[0] if mentions else self.bot.utils.find_member(ctx.guild, user)
            roles = u.roles
            title = title % f'{u.name}#{u.discriminator}'
        else:
            roles = ctx.guild.roles
            title = title % ctx.guild.name
        roles.remove(ctx.guild.default_role)
        await ctx.send(embed=Embed(
            title=title,
            description=' '.join(reversed([r.mention for r in roles])) if roles else 'No roles.',
            colour=roles[len(roles) - 1].colour if roles else 0))

    """
    SELF-ASSIGNABLE ROLE FUNCTIONS
    """

    @commands.Cog.listener()
    async def on_guild_role_delete(self, role):
        db = self.bot.utils.get_server_db(role.guild)
        db.delete_rows(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD), (self.ROLES_TABLE_COL2[0], role.id))
        self.bot.vars.LOGGER.info('[%d] Removed deleted role %s from db.' % (role.guild.id, role.id))

    def _get_sars(self, guild):
        db = self.bot.utils.get_server_db(guild)
        role_ids = db.get_rows(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD))
        return [guild.get_role(r[1]) for r in role_ids if r]

    async def _add_remove_sar(self, ctx, role, which):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        r = self.bot.utils.find_role(ctx.guild, role)
        if r:
            sars = self._get_sars(ctx.guild)
            for sar in sars:
                if sar == r:
                    if which:
                        await ctx.author.add_roles(sar)
                    else:
                        await ctx.author.remove_roles(sar)
                    try:
                        return await self.bot.utils.confirm(ctx)
                    except Forbidden as e:
                        s = 'Added' if which else 'Removed'
                        return await ctx.send(s + ' role. Even though you blocked me.')
            await self.bot.utils.deny(ctx)
        else:
            await ctx.send(self.INVALID_ROLE)

    @commands.command(description='Add a self-assignable *role* to self',
                      help='*role* can be the role name, or id',
                      brief='Add self-role')
    async def iam(self, ctx, *, role=None):
        await self._add_remove_sar(ctx, role, 1)

    @commands.command(description='Remove a self-assignable *role* from self',
                      help='*role* can be the role name or id',
                      brief='Remove self-role')
    async def iamn(self, ctx, *, role=None):
        await self._add_remove_sar(ctx, role, 0)

    @commands.command(description='List all self-assignable roles for the server',
                      help='Listed roles can be added/removed with "iam" command',
                      brief='List self-roles')
    async def lsar(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        sars = sorted(self._get_sars(ctx.guild), reverse=True)
        await ctx.send(embed=Embed(
            title=f'Self-Assignable Roles for `{ctx.guild.name}`',
            description=' '.join([r.mention for r in sars]),
            colour=sars[0].colour if sars else 0))

    @commands.command(description='Add a self-assignable *role* to the server',
                      help='Requires **Manage Guild** or **Manage Roles** permission\n*role* can be the role name or id',
                      brief='Add server self-role')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def asar(self, ctx, *, role):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        db = self.bot.utils.get_server_db(ctx)
        r = self.bot.utils.find_role(ctx.guild, role)
        if r:
            if db.get_row(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD), (self.ROLES_TABLE_COL2[0], r.id)):
                await ctx.send(f'`{r.name}` is already self-assignable.')
            else:
                db.insert_row(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD), (self.ROLES_TABLE_COL2[0], r.id))
                await self.bot.utils.confirm(ctx)
        else:
            await ctx.send(self.INVALID_ROLE)

    @commands.command(description='Remove a self-assignable *role* from the server',
                      help='Requires **Manage Guild** or **Manage Roles** permission\n*role* can be the role name or id',
                      brief='Remove server self-role')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def rsar(self, ctx, *, role):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        db = self.bot.utils.get_server_db(ctx)
        r = self.bot.utils.find_role(ctx.guild, role)
        if r:
            if db.get_row(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD), (self.ROLES_TABLE_COL2[0], r.id)):
                db.delete_rows(self.ROLES_TABLE, (self.ROLES_TABLE_COL1[0], self.SELFROLE_FIELD), (self.ROLES_TABLE_COL2[0], r.id))
                await self.bot.utils.confirm(ctx)
            else:
                await ctx.send(f'`{r.name}` is not self-assignable.')
        else:
            await ctx.send(self.INVALID_ROLE)


async def setup(bot):
    await bot.add_cog(Roles(bot))
