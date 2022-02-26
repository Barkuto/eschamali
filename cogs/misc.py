import random
from discord import Embed, Colour
from discord.errors import NotFound
from discord.ext import commands


class Misc(commands.Cog):
    """Misc commands"""

    def __init__(self, bot):
        self.TEAM_SIZE = 5
        self.STOP = '🛑'
        self.CHECK = '✅'
        self.REROLL = '🎲'
        self.MOVE = '↕️'
        self.RETURN = '⤴️'

        self.bot = bot

    async def get_reaction_users(self, msg, reaction):
        users = None
        for r in msg.reactions:
            if r.emoji == reaction:
                users = await r.users().flatten()
        return users

    def get_embed_teams(self, embed):
        teams = []
        for embed_proxy in embed.fields:
            players = embed_proxy.value.split('\n')
            if len(players) == self.TEAM_SIZE:
                teams += [players]
        return teams

    def get_embed_players(self, embed):
        players = []
        for embed_proxy in embed.fields:
            m = embed_proxy.value.split('\n')
            players += m
        return players

    @commands.Cog.listener()
    async def on_reaction_add(self, reaction, user):
        if user == self.bot.user:
            return
        try:
            if not self.bot.user in [u async for u in reaction.users()]:
                return
        except NotFound:
            return
        msg = reaction.message
        ctx = await self.bot.get_context(msg)
        allowed = await self.bot.is_owner(user) or user.permissions_in(ctx.channel).manage_messages
        if reaction.emoji == self.STOP and allowed:
            users = await self.get_reaction_users(msg, self.CHECK)
            if users:
                players = [u.mention for u in users if not u == self.bot.user]
                await msg.delete()
                await self.inhouse(ctx, *players)
        elif reaction.emoji == self.MOVE and allowed:
            teams = self.get_embed_teams(msg.embeds[0])
            voice_channels = msg.guild.voice_channels
            voice_channels = [v for v in voice_channels if not v.members or v == user.voice.channel]
            if len(teams) > len(voice_channels):
                return await ctx.send('There are not enough empty voice channels to move people.')
            for i in range(0, len(teams)):
                players = teams[i]
                try:
                    players = [int(p.replace('<', '').replace('>', '').replace('!', '').replace('@', '')) for p in players]
                except ValueError:
                    return
                for p in players:
                    member = self.bot.utils.find_member(ctx.guild, p)
                    try:
                        await member.move_to(voice_channels[i])
                    except:
                        pass
        elif reaction.emoji == self.REROLL:
            users = await self.get_reaction_users(msg, self.REROLL)
            if users and msg.embeds:
                players = self.get_embed_players(msg.embeds[0])
                if len(users) - 1 >= len(players) // 2:
                    await msg.clear_reaction(self.REROLL)
                    await self.inhouse(ctx, *players)
        elif reaction.emoji == self.RETURN and allowed:
            teams = self.get_embed_teams(msg.embeds[0])
            voice_channels = msg.guild.voice_channels
            voice_channels = [v for v in voice_channels if not v.members or (user.voice and v == user.voice.channel)]
            if len(voice_channels) < 1:
                return await ctx.send('There are not enough empty voice channels to move people.')
            for players in teams:
                try:
                    players = [int(p.replace('<', '').replace('>', '').replace('!', '').replace('@', '')) for p in players]
                except ValueError:
                    return
                for p in players:
                    member = self.bot.utils.find_member(ctx.guild, p)
                    try:
                        await member.move_to(voice_channels[0])
                    except:
                        pass

    @commands.command(aliases=['ih'],
                      description='Create an in-house from arguments',
                      help='List players separated by a space. Use \'voice\' to use current voice channel users.',
                      brief='Make in-house')
    async def inhouse(self, ctx, *players):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        maps = ['Haven', 'Bind', 'Split', 'Ascent', 'Icebox', 'Breeze', 'Fracture']
        colors = [Colour.orange(), Colour.from_rgb(165, 42, 42), Colour.blue(), Colour.from_rgb(255, 255, 0), Colour.teal(), Colour.dark_teal(), Colour.dark_grey()]
        if len(players) == 1 and players[0].lower() == 'voice':
            voice_state = ctx.author.voice
            if voice_state:
                voice_channel = voice_state.channel
                voice_members = [m.mention for m in voice_channel.members]
                await self.inhouse(ctx, *voice_members)
            else:
                await ctx.send('You must be in a voice channel.')
        else:
            choice = random.randint(0, len(maps) - 1)
            map_choice = maps[choice]
            map_color = colors[choice]
            shuffled_players = list(players)[:]
            random.shuffle(shuffled_players)
            teams = []
            while len(shuffled_players) != 0:
                team = []
                for _ in range(0, self.TEAM_SIZE):
                    if shuffled_players:
                        team.append(shuffled_players.pop(0))
                teams.append(team)

            embed = Embed(title='Map: ' + map_choice,
                          colour=map_color)
            for i in range(0, len(teams)):
                embed.add_field(name='Team ' + str(i + 1),
                                value='\n'.join(teams[i]),
                                inline=True)
            msg = await ctx.send(embed=embed)
            await msg.add_reaction(self.REROLL)
            await msg.add_reaction(self.MOVE)
            await msg.add_reaction(self.RETURN)

    @commands.command(description='Shortcut for \'inhouse voice\' command',
                      help='See \'inhouse\' help.',
                      brief='Voice in-house')
    async def ihv(self, ctx):
        await self.inhouse(ctx, 'voice')

    @commands.command(description='Shortcut for \'inhouse reaction\' command',
                      help='See \'inhouse\' help.',
                      brief='Reaction in-house')
    async def ihr(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        msg = await ctx.send('```React with ✅ to join the in-house.```')
        await msg.add_reaction(self.CHECK)
        await msg.add_reaction(self.STOP)


def setup(bot):
    bot.add_cog(Misc(bot))
