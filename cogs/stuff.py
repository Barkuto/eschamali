import random
from discord import DMChannel
from discord.ext import commands


class Stuff(commands.Cog):

    def __init__(self, bot):
        self.bot = bot
        self.outputs = open('links').readlines()

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
            await ctx.send(self.outputs[random.randrange(len(self.outputs))])
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
