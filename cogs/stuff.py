import random
from discord import DMChannel
from discord.ext import commands

outputs = [
    'https://i.imgur.com/ABnan3T.png',
    'https://i.imgur.com/wNSDttt.jpg',
    'https://streamable.com/qxx1ob',
    'https://www.youtube.com/watch?v=Q16KpquGsIc',
    'https://www.youtube.com/watch?v=qG3GiXsJ4xE',
    'https://www.youtube.com/watch?v=IC5icA_t2f4',
    'https://www.youtube.com/watch?v=D3369OQYUsk',
    'https://i.imgur.com/iEsyCmn.gif',
    'https://i.imgur.com/5GfdutZ.jpg',
    'https://i.imgur.com/oicGMFu.png',
]


class Stuff(commands.Cog):

    def __init__(self, bot):
        self.bot = bot

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        if msg.author.bot:
            return
        ctx = await self.bot.get_context(msg)
        if not ctx.guild.id in [259163710109646849, 459137608451227650]:
            return
        n = random.randint(1, 100)
        if 1 <= n <= 5:
            await ctx.send(outputs[random.randrange(len(outputs))])
        elif n >= 91:
            poss_channels = [c for c in ctx.guild.channels if ctx.author.id in [m.id for m in c.members]]
            random.shuffle(poss_channels)
            m = None
            for c in poss_channels:
                if c.id != ctx.channel.id:
                    try:
                        m = await c.send(msg.author.mention)
                        break
                    except:
                        pass
            if m:
                await m.delete()


def setup(bot):
    bot.add_cog(Stuff(bot))
