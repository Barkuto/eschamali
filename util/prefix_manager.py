DEFAULT_PREFIX = '~'
DEFAULT_HELP_PREFIX = '!'
DEFAULT_PREFIXES = {
    'General': '!',
    'Admin': '/',
    'Perms': ';',
    'Games': '>',
    'CustomCommands': '!',
    'Roles': '.',
    'PAD': '&',
    'Misc': '!'
}


class PrefixManager():
    def __init__(self, bot):
        self.bot = bot
        self.prefix = DEFAULT_PREFIX
        self.help_prefix = DEFAULT_HELP_PREFIX
        self.cog_prefixes = DEFAULT_PREFIXES
        self.cmd_prefixes = {}

    async def get_prefix(self, bot, msg):
        cmd = msg.content.split(' ')[0]
        # just check list of cmds ?
        for _, v in self.cog_prefixes.items():
            if (cmd.startswith(v)):
                cmd = cmd.split(v)[1]
        for _, cmds in self.cmd_prefixes.items():
            for c, p in cmds.items():
                if cmd == c:
                    return p
        return self.prefix

    def load_prefixes(self):
        self.cog_prefixes = {k: v for k, v in sorted(self.cog_prefixes.items(),
                                                     key=lambda item: len(item[1]),
                                                     reverse=True)}
        self.cmd_prefixes = {}
        for name, cog in self.bot.cogs.items():
            self._load_cog_cmd_prefixes(name, cog)
        self.cmd_prefixes['Base'] = {}
        self.cmd_prefixes['Base']['help'] = self.help_prefix
        for cmd in [c for c in self.bot.commands if not c.cog and not c.name == 'help']:
            self.cmd_prefixes['Base'][cmd.name] = self.prefix
            for a in cmd.aliases:
                self.cmd_prefixes['Base'][a] = self.prefix

    def _load_cog_cmd_prefixes(self, name, cog):
        self.cmd_prefixes[name] = {}
        for c in cog.get_commands():
            cmd = c.name
            aliases = c.aliases
            self.cmd_prefixes[name][cmd] = self.cog_prefixes[name]
            for a in aliases:
                self.cmd_prefixes[name][a] = self.cog_prefixes[name]

    def load_cog_cmd_prefixes(self, cog_name):
        for name, cog in self.bot.cogs.items():
            if(name.lower() == cog_name):
                self._load_cog_cmd_prefixes(name, cog)
                break
