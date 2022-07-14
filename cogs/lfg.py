import math
import discord.utils
from discord import Embed, Colour
from discord.ext import commands


class LFG(commands.Cog):
    """LFG commands"""

    def __init__(self, bot):
        self.JOIN_EMOJI = '✅'
        self.LEAVE_EMOJI = '❎'
        self.DELETE_EMOJI = '🗑️'
        self.bot = bot

    @commands.Cog.listener()
    async def on_raw_reaction_add(self, payload):
        channel = self.bot.get_channel(payload.channel_id)
        msg = await channel.fetch_message(payload.message_id)
        user = self.bot.get_user(payload.user_id)
        reaction = discord.utils.get(msg.reactions, emoji=payload.emoji.name)

        if user == self.bot.user or not reaction.message.embeds:
            return
        embed = msg.embeds[0]
        if not embed.title:
            return
        if not self.bot.user in [u async for u in reaction.users()]:
            return
        e = reaction.emoji
        data = self.bot.utils.get_embed_data(embed)
        guild = reaction.message.guild
        if data and 'type' in data:
            if e == self.DELETE_EMOJI and user.id == data['players'][0]:
                await msg.delete()
            elif e == self.JOIN_EMOJI and not user.id in data['players']:
                data['players'] += [user.id]
                leader = self.bot.utils.find_member(guild, data['players'][0])
                self._set_lfg_footer(embed, leader, data)
                embed.description = self._make_player_desc(guild, data['players'])
                await reaction.message.edit(embed=embed)

                if len(data['players']) >= 4:
                    await msg.clear_reactions()
                    mention_message = f'Your Group for `{embed.title}` is: '
                    for u in data['players']:
                        user = self.bot.utils.find_member(guild, u)
                        if user:
                            mention_message += f'{user.mention} '
                    await msg.channel.send(mention_message)
                    await msg.delete()
            elif e == self.LEAVE_EMOJI and user.id in data['players'] and not user.id == data['players'][0]:
                data['players'].remove(user.id)
                leader = self.bot.utils.find_member(guild, data['players'][0])
                self._set_lfg_footer(embed, leader, data)
                embed.description = self._make_player_desc(guild, data['players'])
                await reaction.message.edit(embed=embed)

                if len(data['players']) >= 4:
                    await msg.clear_reactions()
                    mention_message = f'Your Group for `{embed.title}` is: '
                    for u in data['players']:
                        user = self.bot.utils.find_member(guild, u)
                        if user:
                            mention_message += f'{user.mention} '
                    await msg.channel.send(mention_message)
                    await msg.delete()

    @commands.command(description='',
                      help='',
                      brief='LFG')
    async def lfg(self, ctx, *, desc=None):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or not desc:
            return
        print(desc)
        e = Embed()
        e.colour = Colour.orange()
        players = [ctx.author.id]
        data = {
            'type': 'LFG',
            'desc': desc,
            'players': players
        }

        e.title = desc
        e.description = self._make_player_desc(ctx.guild, players)
        self._set_lfg_footer(e, ctx.author, data)

        await ctx.message.delete()
        m = await ctx.send(embed=e)
        await m.add_reaction(self.JOIN_EMOJI)
        await m.add_reaction(self.LEAVE_EMOJI)
        await m.add_reaction(self.DELETE_EMOJI)

    def _make_player_desc(self, guild, user_ids):
        desc = ''
        for i in range(4):
            desc += f'`{i+1}:`'
            if i < len(user_ids):
                member = self.bot.utils.find_member(guild, user_ids[i])
                if member:
                    desc += f'{member.mention}'
            desc += '\n'
        return desc

    def _set_lfg_footer(self, embed, author, data):
        embed.set_footer(text='React Below to Join', icon_url=self.bot.utils.make_data_url(author, data))

    @commands.command(aliases=['b4'],
                      description='',
                      help='',
                      brief='Auction Split for 4')
    async def bid4(self, ctx, market_price: int):
        return await self._bid(ctx, market_price, 4)

    @commands.command(aliases=['b8'],
                      description='',
                      help='',
                      brief='Auction Split for 8')
    async def bid8(self, ctx, market_price: int):
        return await self._bid(ctx, market_price, 8)

    @commands.command(aliases=['b'],
                      description='',
                      help='',
                      brief='Auction Split for n')
    async def bid(self, ctx, players: int, market_price: int):
        return await self._bid(ctx, market_price, players)

    async def _bid(self, ctx, market_price, players):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        return await ctx.send(math.floor(self._calc_bid_split(market_price, players)))

    def _calc_bid_split(self, market_price, players):
        return market_price * 0.95 / players * (players - 1)

    @commands.command(aliases=['p'],
                      description='',
                      help='',
                      brief='Pheon Value Cost')
    async def bid(self, ctx, blue_crystal_price: int):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        return await ctx.send(math.floor(850 / 95 * blue_crystal_price / 100))


def setup(bot):
    bot.add_cog(LFG(bot))
