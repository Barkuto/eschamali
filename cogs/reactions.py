import re
import random
from discord import DMChannel
from discord.ext import commands


class Reactions(commands.Cog):
    """Random situations the bot reacts to"""

    def __init__(self, bot):
        self.ENK = [
            "wtf|why are you awake",
            "why are you awake",
            "shouldn't you be asleep",
            "shouldn't you be studying",
            ":gotosleep:",
            "wtf|:gotosleep:",
            "its past your bedtime",
            "isnt it past your bedtime|why are you awake"
        ]

        self.bot = bot

    @commands.Cog.listener()
    async def on_message(self, msg):
        if isinstance(msg.channel, DMChannel):
            return
        if not self.bot.utils.can_cog_in(self, msg.channel) and msg.author.id != self.bot.user.id:
            return
        if msg.author.bot:
            return
        await self.check_for_reaction(await self.bot.get_context(msg), msg)

    async def check_for_reaction(self, ctx, msg):
        m = msg.content.strip()
        split = [word.lower() for word in re.split(' |\n', m)]
        if 'dont quote me' in m.replace('\'', '').lower():
            await ctx.send(f'"{m.strip()}" - {ctx.author.mention}')
        elif 'alot' in split:
            await ctx.send('https://cdn.discordapp.com/attachments/356568034279686146/1174092586210828410/Alot-vs-a-lot1-600x450.png?ex=656655ee&is=6553e0ee&hm=2f2fed1a99ecb3b82a112e8c0891c72c2b44b19b8c6c9d45e075c5a8aa09f24e&')
        elif 'lambo' in split:
            await ctx.send('https://youtu.be/6w1TieX_I2w')
        # elif msg.author.id == 85844964633747456 and 131547909090050048 in [u.id for u in msg.mentions]:
        elif msg.author.id == 207986006840836097 and 102559179507519488 in [u.id for u in msg.mentions]:
            choice = random.randint(0, len(self.ENK) - 1)
            lines = self.ENK[choice].split('|')
            for l in lines:
                await ctx.send(l.replace(':gotosleep:', '<a:gotosleep:667623052309168128>'))
        elif m.startswith('/o/'):
            await ctx.send('\o\\')
        elif m.startswith('\o\\'):
            await ctx.send('/o/')


async def setup(bot):
    await bot.add_cog(Reactions(bot))
