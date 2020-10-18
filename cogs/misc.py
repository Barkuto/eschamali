import random
from discord import Embed, Colour
from discord.ext import commands


class Misc(commands.Cog):
    """Misc commands"""

    def __init__(self, bot):
        self.bot = bot

    @commands.command(aliases=['ih'],
                      description='Create an in-house from arguments',
                      help='List players separated by a space.',
                      brief='Make in-house')
    async def inhouse(self, ctx, *players):
        maps = ['Haven', 'Bind', 'Split', 'Ascent', 'Icebox']
        colors = [Colour.orange(), Colour.from_rgb(165, 42, 42), Colour.blue(), Colour.from_rgb(255, 255, 0), Colour.teal()]
        if len(players) % 5 == 0:
            choice = random.randint(0, len(maps) - 1)
            map_choice = maps[choice]
            map_color = colors[choice]
            shuffled_players = list(players)[:]
            random.shuffle(shuffled_players)
            teams = []
            while len(shuffled_players) != 0:
                team = []
                for _ in range(0, 5):
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


def setup(bot):
    bot.add_cog(Misc(bot))
