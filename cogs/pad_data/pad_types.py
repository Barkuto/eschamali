import pickle
from enum import Enum


class Monster():
    def __init__(self, dict):
        self.id = dict['id']
        self.name = dict['name']
        self.hp_max = dict['hp_max']
        self.atk_max = dict['atk_max']
        self.rcv_max = dict['rcv_max']
        self.cost = dict['cost']
        self.exp = dict['exp']
        self.rarity = dict['rarity']
        self.lb_mult = dict['lb_mult']
        self.attribute_1_id = dict['attribute_1_id']
        self.attribute_2_id = dict['attribute_2_id']
        self.type_1_id = dict['type_1_id']
        self.type_2_id = dict['type_2_id']
        self.type_3_id = dict['type_3_id']
        self.inheritable = dict['inheritable']
        self.mp = dict['mp']
        self.evolutions = dict['evolutions']
        self.awakenings = dict['awakenings']
        self.supers = dict['supers']
        self.leader = Leader(dict['leader']) if dict['leader'] else None
        self.active = Active(dict['active']) if dict['active'] else None
        self.series = dict['series']
        self.is_animated = dict['has_animation']
        self.latent_slots = dict['latent_slots']

    def __str__(self):
        return f'{self.id} {self.attribute_1_id}/{self.attribute_2_id} \'{self.name}\''

    def __repr__(self):
        return str(self)

    def lb_hp(self):
        return int(self.hp_max * (1 + (self.lb_mult / 100.0)))

    def lb_atk(self):
        return int(self.atk_max * (1 + (self.lb_mult / 100.0)))

    def lb_rcv(self):
        return int(self.rcv_max * (1 + (self.lb_mult / 100.0)))

    def lb_weighted(self):
        return int((self.lb_hp() / 10.0) + (self.lb_atk() / 5.0) + (self.lb_rcv() / 3.0))

    def weighted(self):
        return int((self.hp_max / 10.0) + (self.atk_max / 5.0) + (self.rcv_max / 3.0))

    def get_valid_killer_latents(self):
        l = Type.from_id(self.type_1_id).valid_killer_latents()
        l += Type.from_id(self.type_2_id).valid_killer_latents()
        l += Type.from_id(self.type_3_id).valid_killer_latents()
        return sorted(list(dict.fromkeys(l)), key=lambda l: l.id())


class Leader():
    def __init__(self, dict):
        self.name = dict['leader_name']
        self.desc = dict['leader_desc']
        self.max_hp = dict['max_hp']
        self.max_atk = dict['max_atk']
        self.max_rcv = dict['max_rcv']
        self.max_shield = dict['max_shield']

    def __str__(self):
        return f'{self.name}: {self.desc}'

    def __repr__(self):
        return str(self)


class Active():
    def __init__(self, dict):
        self.name = dict['active_name']
        self.desc = dict['active_desc']
        self.turn_max = dict['turn_max']
        self.turn_min = dict['turn_min']

    def __str__(self):
        return f'{self.name}: {self.desc}'

    def __repr__(self):
        return str(self)


class Attribute(Enum):
    NONE = None
    FIRE = 0
    WATER = 1
    WOOD = 2
    LIGHT = 3
    DARK = 4

    def id(self):
        return self.value

    def __str__(self):
        if self == Attribute.FIRE:
            return 'r'
        elif self == Attribute.WATER:
            return 'b'
        elif self == Attribute.WOOD:
            return 'g'
        elif self == Attribute.LIGHT:
            return 'l'
        elif self == Attribute.DARK:
            return 'd'
        else:
            return 'x'

    def __repr__(self):
        return str(self)

    @ classmethod
    def from_id(cls, att_id):
        for a in cls:
            if a.value == att_id:
                return a
        return cls.NONE

    @ classmethod
    def from_str(cls, att_str):
        if att_str == 'r':
            return cls.FIRE
        elif att_str == 'b':
            return cls.WATER
        elif att_str == 'g':
            return cls.WOOD
        elif att_str == 'l':
            return cls.LIGHT
        elif att_str == 'd':
            return cls.DARK
        else:
            return cls.NONE


class Type(Enum):
    NONE = (None, '')
    EVOMAT = (0, 'Evo Material')
    BALANCED = (1, 'Balanced')
    PHYSICAL = (2, 'Physical')
    HEALER = (3, 'Healer')
    DRAGON = (4, 'Dragon')
    GOD = (5, 'God')
    ATTACKER = (6, 'Attacker')
    DEVIL = (7, 'Devil')
    MACHINE = (8, 'Machine')
    AWOKENMAT = (12, 'Awoken Skill Material')
    ENHANCEMAT = (14, 'Enhance Material')
    VENDORMAT = (15, 'Vendor Material')

    def id(self):
        return self.value[0]  # pylint: disable=unsubscriptable-object

    def name(self):
        return self.value[1]  # pylint: disable=unsubscriptable-object

    def __str__(self):
        return str(self.name())

    def __repr__(self):
        return str(self)

    def valid_killer_latents(self):
        if self == Type.DRAGON:
            return [Type.HEALER, Type.MACHINE]
        elif self == Type.GOD:
            return [Type.DEVIL]
        elif self == Type.ATTACKER:
            return [Type.PHYSICAL, Type.DEVIL]
        elif self == Type.DEVIL:
            return [Type.GOD]
        elif self == Type.PHYSICAL:
            return [Type.HEALER, Type.MACHINE]
        elif self == Type.HEALER:
            return [Type.DRAGON, Type.ATTACKER]
        elif self == Type.MACHINE:
            return [Type.BALANCED, Type.GOD]
        elif self == Type.BALANCED:
            return [t for t in Type][2:-3]
        else:
            return []

    @ classmethod
    def from_id(cls, type_id):
        for t in cls:
            if t.id() == type_id:
                return t
        return cls.NONE


class Awakening(Enum):
    UNKNOWN = (0, "??", "??")

    HP = (1, "Enhanced HP", "+HP")
    ATK = (2, "Enhanced Attack", "+ATK")
    RCV = (3, "Enhanced Heal", "+RCV")

    FIRERES = (4, "Reduce Fire Damage", "FireRes")
    WATERRES = (5, "Reduce Water Damage", "WaterRes")
    WOODRES = (6, "Reduce Wood Damage", "WoodRes")
    LIGHTRES = (7, "Reduce Light Damage", "LightRes")
    DARKRES = (8, "Reduce Dark Damage", "DarkRes")

    AUTORCV = (9, "Auto-Recover", "Auto-RCV")
    BINDRES = (10, "Resistance-Bind", "BindRes")
    BLINDRES = (11, "Resistance-Dark", "BlindRes")
    JAMRES = (12, "Resistance-Jammers", "JamRes")
    POIRES = (13, "Resistance-Poison", "PoiRes")

    FIREOE = (14, "Enhanced Fire Orbs", "FireOE")
    WATEROE = (15, "Enhanced Water Orbs", "WaterOE")
    WOODOE = (16, "Enhanced Wood Orbs", "WoodOE")
    LIGHTOE = (17, "Enhanced Light Orbs", "LightOE")
    DARKOE = (18, "Enhanced Dark Orbs", "DarkOE")
    HEARTOE = (29, "Enhanced Heart Orbs", "HeartOE")

    TIMEEXTEND = (19, "Extend Time", "TE")
    BINDRCV = (20, "Recover Bind", "BindRCV")
    SKILLBOOST = (21, "Skill Boost", "SB")
    TPA = (27, "Two-Pronged Attack", "TPA")
    SBR = (28, "Resistance-Skill Lock", "SBR")

    FIREROW = (22, "Enhanced Fire Att.", "FireRow")
    WATERROW = (23, "Enhanced Water Att.", "WaterRow")
    WOODROW = (24, "Enhanced Wood Att.", "WoodRow")
    LIGHTROW = (25, "Enhanced Light Att.", "LightRow")
    DARKROW = (26, "Enhanced Dark Att.", "DarkRow")

    COOP = (30, "Multi Boost", "CoopBoost")

    DRAGONKILLER = (31, "Dragon Killer", "DragonKill")
    GODKILLER = (32, "God Killer", "GodKill")
    DEVILKILLER = (33, "Devil Killer", "DevilKill")
    MACHINEKILLER = (34, "Machine Killer", "MachineKill")
    BALANCEDKILLER = (35, "Balanced Killer", "BalancedKill")
    ATTACKERKILLER = (36, "Attacker Killer", "AttackerKill")
    PHYSICALKILLER = (37, "Physical Killer", "PhysicalKill")
    HEALERKILLER = (38, "Healer Killer", "HealerKill")

    EVOMATKILLER = (39, "Evo Material Killer", "EvoKill")
    AWOKENMATKILLER = (40, "Awaken Material Killer", "AwakenKill")
    ENHANCEKILLER = (41, "Enhance Material Killer", "EnhanceKill")
    VENDORKILLER = (42, "Redeemable Material Killer", "RedeemKill")

    SEVENC = (43, "Enhanced Combo", "7cBoost")
    GUARDBREAK = (44, "Guard Break", "DefBreak")
    FOLLOWUPATTACK = (45, "Follow Up Attack", "FUA")

    TEAMHP = (46, "Team HP+", "HP+")
    TEAMRCV = (47, "Team RCV+", "RCV+")

    VOIDPEN = (48, "Void Penetration", "VoidPen")
    ASSIST = (49, "Awoken Assist", "Assist")
    SFUA = (50, "Super Follow Up Attack", "SFUA")
    CHARGE = (51, "Skill Charge", "Charge")

    BINDRESPLUS = (52, "Bind Res Plus", "BindRes+")
    TEPLUS = (53, "TE Plus", "TE+")
    CLOUDRES = (54, "Cloud Resist", "CloudRes")
    RIBBONRES = (55, "Ribbon Resist", "RibbonRes")
    SBPLUS = (56, "SB Plus", "SB+")
    HP80BOOST = (57, "HP > 80 Boost", "80Boost")
    HP50BOOST = (58, "HP < 50 Boost", "50Boost")
    LSHIELD = (59, "L Shield", "LShield")
    LUNLOCK = (60, "L Unlock", "LUnlock")

    TENC = (61, "Greatly Enhanced Combo", "10cBoost")

    COMBOORB = (62, "Combo Orb", "ComboOrb")
    VOICE = (63, "Skill Voice", "Voice")
    DUNGEONBOOST = (64, "Dungeon Bonus", "DungeonBoost")

    MINUSHP = (65, "Reduced HP", "MinusHP")
    MINUSATK = (66, "Reduced Attack", "MinusATK")
    MINUSRCV = (67, "Reduced RCV", "MinusRCV")

    BLINDRESPLUS = (68, "Blind Resist Plus", "BlindResPlus")
    JAMMERRESPLUS = (69, "Jammer Resist Plus", "JammerResPlus")
    POISONRESPLUS = (70, "Poison Resist Plus", "PoisonResPlus")

    JAMMERBLESSING = (71, "Blessing of Jammer", "JammerBlessing")
    POISONBLESSING = (72, "Blessing of Poison", "PoisonBlessing")

    def id(self):
        return self.value[0]  # pylint: disable=unsubscriptable-object

    def name(self):
        return self.value[1]  # pylint: disable=unsubscriptable-object

    def short_name(self):
        return self.value[2]  # pylint: disable=unsubscriptable-object

    def __str__(self):
        return str(self.id())

    def __repr__(self):
        return str(self)

    @ classmethod
    def from_id(cls, aw_id):
        for aw in cls:
            if aw.id() == aw_id:
                return aw
        return cls.UNKNOWN
