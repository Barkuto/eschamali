import re
import random
import importlib
from discord import Embed, DMChannel
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')

enk = [
    "wtf|why are you awake",
    "why are you awake",
    "shouldn't you be asleep",
    "shouldn't you be studying",
    ":gotosleep:",
    "wtf|:gotosleep:",
    "its past your bedtime",
    "isnt it past your bedtime|why are you awake"
]


class Reactions(commands.Cog):
    """Random situations the bot reacts to"""

    def __init__(self, bot):
        self.bot = bot

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        if not UTILS.can_cog_in(self, msg.channel) and msg.author.id != self.bot.user.id:
            return
        if msg.author.bot:
            return
        await self.check_for_reaction(await self.bot.get_context(msg), msg)

    async def check_for_reaction(self, ctx, msg):
        m = msg.content
        split = re.split(' |\n', m)
        if 'dont quote me' in m.replace('\'', '').lower():
            await ctx.send(f'"{m.strip()}" - {ctx.author.mention}')
        elif 'alot' in split:
            await ctx.send('http://thewritepractice.com/wp-content/uploads/2012/05/Alot-vs-a-lot1-600x450.png')
        elif 'lambo' in split:
            await ctx.send('https://streamable.com/qxx1ob')
        # elif msg.author.id == 85844964633747456 and 131547909090050048 in [u.id for u in msg.mentions]:
        elif msg.author.id == 207986006840836097 and 102559179507519488 in [u.id for u in msg.mentions]:
            choice = random.randint(0, len(enk) - 1)
            lines = enk[choice].split('|')
            for l in lines:
                await ctx.send(l.replace(':gotosleep:', '<a:gotosleep:667623052309168128>'))


def setup(bot):
    bot.add_cog(Reactions(bot))
