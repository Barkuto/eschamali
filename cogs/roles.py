import importlib
from discord import Embed
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = UTILS.DB
LOGGER = UTILS.VARS.LOGGER

ROLES_TABLE = 'roles'
ROLES_TABLE_COL1 = ('field', DB_MOD.TEXT)
ROLES_TABLE_COL2 = ('role', DB_MOD.INTEGER)

AUTOROLE_FIELD = 'autorole'
SELFROLE_FIELD = 'selfrole'

INVALID_ROLE = 'Invalid role.'


class Roles(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
        for guild in bot.guilds:
            self._init_db(guild)

    def _init_db(self, guild):
        db = UTILS.get_server_db(guild)
        db.create_table(ROLES_TABLE, ROLES_TABLE_COL1, ROLES_TABLE_COL2)

    """
    AUTOROLE FUNCTIONS
    """

    @commands.Cog.listener()
    async def on_guild_join(self, guild):
        self._init_db(guild)

    def _get_autorole(self, guild):
        db = UTILS.get_server_db(guild)
        role_id = db.get_value(ROLES_TABLE, ROLES_TABLE_COL2[0], (ROLES_TABLE_COL1[0], AUTOROLE_FIELD))
        return guild.get_role(role_id)

    def _set_autorole(self, role):
        db = UTILS.get_server_db(role.guild)
        self._remove_autorole(role.guild)
        db.insert_row(ROLES_TABLE, (ROLES_TABLE_COL1[0], AUTOROLE_FIELD), (ROLES_TABLE_COL2[0], role.id))

    def _remove_autorole(self, guild):
        db = UTILS.get_server_db(guild)
        db.delete_rows(ROLES_TABLE, (ROLES_TABLE_COL1[0], AUTOROLE_FIELD))

    @commands.Cog.listener()
    async def on_member_join(self, member):
        if not UTILS.is_cog_enabled(member.guild, self):
            return
        autorole = self._get_autorole(member.guild)
        if autorole:
            await member.add_roles(autorole)

    @commands.command()
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def autorole(self, ctx, *, role=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.role_mentions
        if role and mentions:
            self._set_autorole(mentions[0])
            return await UTILS.confirm(ctx)
        elif role:
            new_role = UTILS.find_role(ctx.guild, role)
            if new_role:
                self._set_autorole(new_role)
                return await UTILS.confirm(ctx)
            return await ctx.send(INVALID_ROLE)

        role = self._get_autorole(ctx.guild)
        await ctx.send(embed=Embed(
            title=f'Autorole for `{ctx.guild}`',
            description=role.mention if role else 'None.',
            colour=role.colour if role else 0))

    @commands.command(aliases=['raa'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def removeautorole(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        self._remove_autorole(ctx.guild)
        return await UTILS.confirm(ctx)

    """
    BASIC ROLE FUNCTIONS
    """

    @commands.command(aliases=['ar'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def addrole(self, ctx, user, *, role=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.mentions
        if mentions:
            u = mentions[0]
            r = UTILS.find_role(ctx.guild, role)
            if r:
                await u.add_roles(r)
                await UTILS.confirm(ctx)

    @commands.command(aliases=['rr'])
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def removerole(self, ctx, user, *, role=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        mentions = ctx.message.mentions
        if mentions:
            u = mentions[0]
            r = UTILS.find_role(ctx.guild, role)
            if r:
                await u.remove_roles(r)
                await UTILS.confirm(ctx)

    @commands.command()
    async def inrole(self, ctx, *, role=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        r = UTILS.find_role(ctx.guild, role)
        in_role = [m.mention for m in ctx.guild.members if r in m.roles]
        await ctx.send(embed=Embed(
            title=f'`{r.name}`',
            description=' '.join(in_role),
            colour=r.colour))

    @commands.command()
    async def roles(self, ctx, *, user=None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        roles = []
        title = 'Roles for `%s`'
        mentions = ctx.message.mentions
        if user or mentions:
            u = mentions[0] if mentions else UTILS.find_member(ctx.guild, user)
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
        db = UTILS.get_server_db(role.guild)
        db.delete_rows(ROLES_TABLE, (ROLES_TABLE_COL1[0], AUTOROLE_FIELD), (ROLES_TABLE_COL2[0], role.id))
        LOGGER.info('[%d] Removed deleted role %s from db.' % (role.guild.id, role.id))

    def _get_sars(self, guild):
        db = UTILS.get_server_db(guild)
        role_ids = db.get_rows(ROLES_TABLE, (ROLES_TABLE_COL1[0], SELFROLE_FIELD))
        return [guild.get_role(r[1]) for r in role_ids if r]

    async def _add_remove_sar(self, ctx, role, which):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        r = UTILS.find_role(ctx.guild, role)
        if r:
            sars = self._get_sars(ctx.guild)
            for sar in sars:
                if sar == r:
                    if which:
                        await ctx.author.add_roles(sar)
                    else:
                        await ctx.author.remove_roles(sar)
                    return await UTILS.confirm(ctx)
            await UTILS.deny(ctx)
        else:
            await ctx.send(INVALID_ROLE)

    @commands.command()
    async def iam(self, ctx, *, role=None):
        await self._add_remove_sar(ctx, role, 1)

    @commands.command()
    async def iamn(self, ctx, *, role=None):
        await self._add_remove_sar(ctx, role, 0)

    @commands.command()
    async def lsar(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        sars = sorted(self._get_sars(ctx.guild), reverse=True)
        await ctx.send(embed=Embed(
            title=f'Self-Assignable Roles for `{ctx.guild.name}`',
            description=' '.join([r.mention for r in sars]),
            colour=sars[0].colour if sars else 0))

    @commands.command()
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def asar(self, ctx, *, role):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        r = UTILS.find_role(ctx.guild, role)
        if r:
            if db.get_row(ROLES_TABLE, (ROLES_TABLE_COL1[0], SELFROLE_FIELD), (ROLES_TABLE_COL2[0], r.id)):
                await ctx.send(f'`{r.name}` is already self-assignable.')
            else:
                db.insert_row(ROLES_TABLE, (ROLES_TABLE_COL1[0], SELFROLE_FIELD), (ROLES_TABLE_COL2[0], r.id))
                await UTILS.confirm(ctx)
        else:
            await ctx.send(INVALID_ROLE)

    @commands.command()
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True),
                        commands.has_permissions(manage_roles=True))
    async def rsar(self, ctx, *, role):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = UTILS.get_server_db(ctx)
        r = UTILS.find_role(ctx.guild, role)
        if r:
            if db.get_row(ROLES_TABLE, (ROLES_TABLE_COL1[0], SELFROLE_FIELD), (ROLES_TABLE_COL2[0], r.id)):
                db.delete_rows(ROLES_TABLE, (ROLES_TABLE_COL1[0], SELFROLE_FIELD), (ROLES_TABLE_COL2[0], r.id))
                await UTILS.confirm(ctx)
            else:
                await ctx.send(f'`{r.name}` is not self-assignable.')
        else:
            await ctx.send(INVALID_ROLE)


def setup(bot):
    bot.add_cog(Roles(bot))
