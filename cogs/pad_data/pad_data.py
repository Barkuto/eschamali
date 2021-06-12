import os
import os.path
import importlib
import requests
import pickle
import sqlite3
from sqlite3 import Binary
from util.db import DB
from cogs.pad_data import pad_types

Monster = pad_types.Monster
Leader = pad_types.Leader
Active = pad_types.Active
Attribute = pad_types.Attribute
Type = pad_types.Type
Awakening = pad_types.Awakening

CARDS_DB = os.path.join(os.path.dirname(__file__), 'cards_%s.db')
CARDS_TMP_DB = os.path.join(os.path.dirname(__file__), '_cards_%s.db')

DADGUIDEURL = 'https://d1kpnpud0qoyxf.cloudfront.net/db/dadguide.sqlite'
DADGUIDEDB = os.path.join(os.path.dirname(__file__), 'dadguide.sqlite')

NA = 'na'
JP = 'jp'

EQUIP_KEYWORDS = ['equip', 'e']
BASE_KEYWORDS = ['base']
MAX_KEYWORDS = ['max', 'high', 'highest']
KEYWORDS = EQUIP_KEYWORDS + BASE_KEYWORDS + MAX_KEYWORDS


def reload():
    global Monster, Leader, Active, Attribute, Type, Awakening
    importlib.reload(pad_types)

    Monster = pad_types.Monster
    Leader = pad_types.Leader
    Active = pad_types.Active
    Attribute = pad_types.Attribute
    Type = pad_types.Type
    Awakening = pad_types.Awakening


"""
DB UPDATING/PROCESSING
"""


def _process_db():
    req = requests.get(DADGUIDEURL)
    with open(DADGUIDEDB, 'wb') as f:
        f.write(req.content)

    dgdb = sqlite3.connect(DADGUIDEDB)
    memory_db = sqlite3.connect(':memory:')
    dgdb.backup(memory_db)
    dgdb.close()
    memory_db.row_factory = sqlite3.Row

    series = {}
    for r in memory_db.execute(f'SELECT * FROM series'):
        series[r['series_id']] = r[f'name_en']

    monsters = {NA: [], JP: []}
    for region, mons in monsters.items():
        monster_table = 'monsters' + ('_na' if region == NA else '')
        leader_table = 'leader_skills' + ('_na' if region == NA else '')
        active_table = 'active_skills' + ('_na' if region == NA else '')
        awakenings_table = 'awakenings' + ('_na' if region == NA else '')
        evolutions_table = 'evolutions' + ('_na' if region == NA else '')
        for r in memory_db.execute(f'SELECT * FROM {monster_table} WHERE on_{region}=1'):
            mons_id = r['monster_id']
            m = {
                'id': r[f'monster_no_{region}'],
                'name': r['name_en_override'] or r['name_en'],
                'hp_max': r['hp_max'],
                'atk_max': r['atk_max'],
                'rcv_max': r['rcv_max'],
                'cost': r['cost'],
                'max_level': r['level'],
                'exp': r['exp'],
                'rarity': r['rarity'],
                'lb_mult': r['limit_mult'],
                'attribute_1_id': r['attribute_1_id'],
                'attribute_2_id': r['attribute_2_id'] if not r['attribute_2_id'] is None else Attribute.NONE.id(),
                'type_1_id': r['type_1_id'],
                'type_2_id': r['type_2_id'],
                'type_3_id': r['type_3_id'],
                'inheritable': r['inheritable'],
                'mp': r['sell_mp'],
                'evolutions': [],
                'awakenings': [],
                'supers': [],
                'leader': {},
                'active': {},
                'series': series[r['series_id']],
                'has_animation': r['has_animation'],
                'latent_slots': r['latent_slots']
            }

            ls_id = r['leader_skill_id']
            ls_row = memory_db.execute(f'SELECT * FROM {leader_table} WHERE leader_skill_id={ls_id}').fetchone()
            if ls_row:
                m['leader'] = {
                    'leader_name': ls_row[f'name_en'],
                    'leader_desc': ls_row[f'desc_en'],
                    'max_hp': ls_row['max_hp'],
                    'max_atk': ls_row['max_atk'],
                    'max_rcv': ls_row['max_rcv'],
                    'max_shield': ls_row['max_shield']
                }

            as_id = r['active_skill_id']
            as_row = memory_db.execute(f'SELECT * FROM {active_table} WHERE active_skill_id={as_id}').fetchone()
            if as_row:
                m['active'] = {
                    'active_name': as_row[f'name_en'],
                    'active_desc': as_row[f'desc_en'],
                    'turn_max': as_row['turn_max'],
                    'turn_min': as_row['turn_min'],
                }

            awakenings = memory_db.execute(f'SELECT * FROM {awakenings_table} WHERE monster_id={mons_id}')
            for aw_row in awakenings:
                awoken_skill_id = aw_row['awoken_skill_id']
                is_super = aw_row['is_super']
                order_idx = aw_row['order_idx']
                if is_super:
                    m['supers'].append((order_idx, awoken_skill_id))
                else:
                    m['awakenings'].append((order_idx, awoken_skill_id))
            m['supers'] = [s[1] for s in sorted(m['supers'], key=lambda t:t[0])]
            m['awakenings'] = [s[1] for s in sorted(m['awakenings'], key=lambda t:t[0])]

            evos_from = memory_db.execute(f'SELECT * FROM {evolutions_table} WHERE from_id={mons_id}')
            evos_to = memory_db.execute(f'SELECT * FROM {evolutions_table} WHERE to_id={mons_id}')
            queue = [e['to_id'] for e in evos_from] + [e['from_id'] for e in evos_to]
            linked_mons_from = memory_db.execute(f'SELECT * FROM {monster_table} WHERE linked_monster_id={mons_id}')
            if r['linked_monster_id']:
                queue += [r['linked_monster_id']]
            if linked_mons_from:
                queue += [r['monster_id'] for r in linked_mons_from]

            all_evos = []
            while queue:
                n = queue.pop(0)
                if not n in all_evos:
                    queue += [r['to_id'] for r in memory_db.execute(f'SELECT * FROM {evolutions_table} WHERE from_id={n}') if r['to_id']]
                    queue += [r['from_id'] for r in memory_db.execute(f'SELECT * FROM {evolutions_table} WHERE to_id={n}') if r['from_id']]
                    queue += [r['linked_monster_id'] for r in memory_db.execute(f'SELECT * FROM {monster_table} WHERE monster_id={n}') if r['linked_monster_id']]
                    queue += [r['monster_id'] for r in memory_db.execute(f'SELECT * FROM {monster_table} WHERE linked_monster_id={n}') if r['monster_id']]
                    all_evos.append(n)
            all_evos = [e for e in filter(lambda e: memory_db.execute(f'SELECT * FROM {monster_table} WHERE monster_id={e} AND on_{region}=1').fetchone(), all_evos)]
            if mons_id in all_evos:
                all_evos.remove(mons_id)
            all_evos = [memory_db.execute(f'SELECT * FROM {monster_table} WHERE monster_id={e}').fetchone()[f'monster_no_{region}'] for e in all_evos]
            m['evolutions'] = sorted(all_evos)

            mons.append(Monster(m))
    os.remove(DADGUIDEDB)
    memory_db.close()
    return monsters


def update_monsters():
    all_monsters = _process_db()
    for region, monsters in all_monsters.items():
        memory_db = sqlite3.connect(':memory:')
        memory_db.execute('DROP TABLE IF EXISTS monsters')
        memory_db.execute('CREATE TABLE IF NOT EXISTS monsters (id integer, name text, att1 text, att2 text, series text, monster blob)')

        for m in monsters:
            if 0 < m.id < 100000:
                memory_db.execute('INSERT INTO monsters (id, name, att1, att2, series, monster) VALUES (?,?,?,?,?,?)',
                                  (m.id, m.name, m.attribute_1_id, m.attribute_2_id, m.series, mons_to_binary(m)))
            memory_db.commit()
        local_db = sqlite3.connect(CARDS_TMP_DB % region)
        memory_db.backup(local_db)
        local_db.close()
        memory_db.close()
        os.replace(CARDS_TMP_DB % region, CARDS_DB % region)


def mons_to_binary(monster):
    return Binary(pickle.dumps(monster, -1))


def binary_to_mons(binary):
    return pickle.loads(binary)


def verify_region(region):
    return region.lower() == NA or region.lower() == JP


"""
GET MONSTERS
"""


def _get_monster(id, region):
    db = DB(CARDS_DB % region)
    m_bytes = db.get_value('monsters', 'monster', ('id', id))
    if m_bytes:
        return binary_to_mons(m_bytes)
    return None


def get_monster(query, region):
    monsters = get_monsters(query, region)
    best_weight = -999.0
    highest_weight_mons = None
    for m in monsters:
        to_check = m.weighted()
        if to_check < 0:
            new_hp = abs(m.hp_max)
            new_atk = abs(m.atk_max)
            new_rcv = abs(m.rcv_max)
            to_check = (new_hp / 10.0) + (new_atk / 5.0) + (new_rcv / 3.0)
        if to_check > best_weight:
            highest_weight_mons = m
            best_weight = m.weighted()
    return highest_weight_mons


def get_monsters(query, region):
    if query and verify_region(region):
        if isinstance(query, int) or query.isdigit():
            m = _get_monster(int(query), region)
            return [m] if m else []
        else:
            mons_to_use = []
            db = DB(CARDS_DB % region)
            query, atts = parse_attribute(query.lower())
            query, keyword = parse_keyword(query)
            split = query.split(' ')
            match_cols = ()
            for i in range(0, len(atts)):
                match_cols += ((f'att{i+1}', atts[i].id()),)
            like_cols = tuple([('name', s) for s in split])
            results = [binary_to_mons(m) for _, _, _, _, _, m in
                       db.get_rows_like('monsters',
                                        match_cols=match_cols,
                                        like_cols=like_cols,
                                        factory=True)]
            exact_names = []
            for m in results:
                if m.name.lower() == query:
                    mons_to_use = [m] + [mons for mons in [_get_monster(m_id, region) for m_id in m.evolutions] if query in mons.name.lower()]
                    break
                name_split = m.name.lower().replace(',', '').replace('\'s', '').replace('\'', '').split(' ')
                found = 0
                for s in split:
                    for n in name_split:
                        if s == n:
                            found += 1
                if found == len(split):
                    exact_names.append(m)
            mons_to_use = mons_to_use or exact_names or results
            if mons_to_use and keyword:
                kw_matches = []
                if keyword in EQUIP_KEYWORDS:
                    for m in mons_to_use:
                        to_check = [m] + [_get_monster(m_id, region) for m_id in m.evolutions]
                        for evo_mons in to_check:
                            if Awakening.ASSIST.id() in evo_mons.awakenings:
                                kw_matches += [evo_mons]
                elif keyword in BASE_KEYWORDS:
                    test_mons = mons_to_use[0]
                    if test_mons.evolutions and test_mons.evolutions[0] < test_mons.id:
                        return [_get_monster(test_mons.evolutions[0], region)]
                    return [mons_to_use[0]]
                elif keyword in MAX_KEYWORDS:
                    test_mons = mons_to_use[len(mons_to_use)-1]
                    if test_mons.evolutions and test_mons.evolutions[len(test_mons.evolutions)-1] > test_mons.id:
                        return [_get_monster(test_mons.evolutions[len(test_mons.evolutions)-1], region)]
                    return [mons_to_use[len(mons_to_use)-1]]
                return kw_matches
            else:
                return mons_to_use
    return []


def get_series(series, region):
    if verify_region(region):
        db = DB(CARDS_DB % region)
        like_cols = tuple([('series', s) for s in series.split(' ')])
        results = db.get_rows_like('monsters',
                                   like_cols=like_cols,
                                   factory=True)
        return [binary_to_mons(m) for _, _, _, _, _, m in results]
    return []


def parse_attribute(query):
    atts = ()
    for s in query.split(' '):
        if 0 < len(s) <= 2:
            for c in s:
                if not c in KEYWORDS:
                    atts += (Attribute.from_str(c),)
            if len(atts) > 0:
                break
    return (remove_atts(atts, query), atts)


def remove_atts(atts, query):
    att = ''
    for a in atts:
        att += str(a)
    new_query = []
    for s in query.split(' '):
        if att != s:
            new_query.append(s)
    return ' '.join(new_query)


def parse_keyword(query):
    for s in query.split(' '):
        if s in KEYWORDS:
            return (remove_keyword(s, query), s)
    return (query, '')


def remove_keyword(keyword, query):
    new_query = []
    for s in query.split(' '):
        if s != keyword:
            new_query += [s]
    return ' '.join(new_query)


"""
PICTURES
"""
full_pic_url = 'https://d1kpnpud0qoyxf.cloudfront.net/media/portraits/%05d.png'
portrait_pic_url = 'https://d1kpnpud0qoyxf.cloudfront.net/media/icons/%05d.png'
animated_mp4_url = 'https://d1kpnpud0qoyxf.cloudfront.net/media/animated_portraits/%05d.mp4'
animated_gif_url = 'https://d1kpnpud0qoyxf.cloudfront.net/media/animated_portraits/%05d.gif'


def _get_pic_url_search(query, region, pic_type):
    m = get_monster(query, region)
    url = _get_pic_url_for(m, region, pic_type)
    if m and url:
        return url
    return None


def _get_pic_url_for(m: Monster, region, pic_type):
    if m:
        url = None
        is_animated = m.is_animated
        if pic_type == 'MP4':
            if is_animated:
                url = animated_mp4_url
        elif pic_type == 'GIF':
            if is_animated:
                url = animated_gif_url
        elif pic_type == 'PORTRAIT':
            url = portrait_pic_url
        else:
            url = full_pic_url
        if url:
            if region.lower() == JP:
                url = url % m.id
            else:
                url = url % na_no_to_monster_id(m.id)
            return url
    return None


def _get_pic_url(obj, region, pic_type):
    f = _get_pic_url_search
    if isinstance(obj, Monster):
        f = _get_pic_url_for
    return f(obj, region, pic_type)


def get_portrait_url(obj, region):
    return _get_pic_url(obj, region, pic_type='PORTRAIT')


def get_picture_url(obj, region):
    return _get_pic_url(obj, region, pic_type='FULL')


def get_animated_mp4_url(obj, region):
    return _get_pic_url(obj, region, pic_type='MP4')


def get_animated_gif_url(obj, region):
    return _get_pic_url(obj, region, pic_type='GIF')

# https://github.com/TsubakiBotPad/pad-data-pipeline/blob/master/etl/pad/common/monster_id_mapping.py


def between(n, bottom, top):
    return bottom <= n <= top


def adjust(n, local_bottom, remote_bottom):
    return n - local_bottom + remote_bottom


def na_no_to_monster_id(na_id):
    # Shinra Bansho 1
    if between(na_id, 934, 935):
        return adjust(na_id, 934, 669)

    # Shinra Bansho 2
    if between(na_id, 1049, 1058):
        return adjust(na_id, 1049, 671)

    # Batman 1
    if between(na_id, 669, 680):
        return adjust(na_id, 669, 924)

    # Batman 2
    if between(na_id, 924, 933):
        return adjust(na_id, 924, 1049)

    # Voltron
    if between(na_id, 2601, 2631):
        return adjust(na_id, 2601, 2601 + 10000)

    # Power Rangers
    if between(na_id, 4949, 4987):
        return adjust(na_id, 4949, 4949 + 10000)

    return na_id
