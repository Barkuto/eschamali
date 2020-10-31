import random
from discord import Embed, Colour
from discord.ext import commands


class Misc(commands.Cog):
    """Misc commands"""

    def __init__(self, bot):
        self.bot = bot

    @commands.command(aliases=['ih'],
                      description='Create an in-house from arguments',
                      help='List players separated by a space. Use \'voice\' to use current voice channel users.',
                      brief='Make in-house')
    async def inhouse(self, ctx, *players):
        maps = ['Haven', 'Bind', 'Split', 'Ascent', 'Icebox']
        colors = [Colour.orange(), Colour.from_rgb(165, 42, 42), Colour.blue(), Colour.from_rgb(255, 255, 0), Colour.teal()]
        if len(players) == 1 and players[0].lower() == 'voice':
            voice_state = ctx.author.voice
            if voice_state:
                voice_channel = voice_state.channel
                voice_members = [m.mention for m in voice_channel.members]
                await self.inhouse(ctx, *voice_members)
            else:
                await ctx.send('You must be in a voice channel.')
        elif players:
            choice = random.randint(0, len(maps) - 1)
            map_choice = maps[choice]
            map_color = colors[choice]
            shuffled_players = list(players)[:]
            random.shuffle(shuffled_players)
            teams = []
            while len(shuffled_players) != 0:
                team = []
                for _ in range(0, 5):
                    if shuffled_players:
                        team.append(shuffled_players.pop(0))
                teams.append(team)

            embed = Embed(title='Map: ' + map_choice,
                          colour=map_color)
            for i in range(0, len(teams)):
                embed.add_field(name='Team ' + str(i + 1),
                                value='\n'.join(teams[i]),
                                inline=True)
            await ctx.send(embed=embed)
        else:
            await ctx.send('Players not a multiple of 5.')

    @commands.command(description='Shortcut for \'inhouse voice\' command',
                      help='See \'inhouse\' help.',
                      brief='Voice in-house')
    async def ihv(self, ctx):
        await self.inhouse(ctx, 'voice')


def setup(bot):
    bot.add_cog(Misc(bot))
