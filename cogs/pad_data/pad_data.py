import os
import os.path
import importlib
import urllib.request
import pickle
from sqlite3 import Binary

UTILS = importlib.import_module('.utils', 'util')
PAD_TYPES = importlib.import_module('.pad_types', 'cogs.pad_data')
DB_MOD = UTILS.DB_MOD
DB = UTILS.DB

Monster = PAD_TYPES.Monster
Attribute = PAD_TYPES.Attribute
Type = PAD_TYPES.Type
Awakening = PAD_TYPES.Awakening

CARDS_DB = os.path.join(os.path.dirname(__file__), 'cards_%s.db')
CARDS_TMP_DB = os.path.join(os.path.dirname(__file__), '_cards_%s.db')

DADGUIDEURL = 'https://d1kpnpud0qoyxf.cloudfront.net/db/dadguide.sqlite'
DADGUIDEDB = os.path.join(os.path.dirname(__file__), 'dadguide.sqlite')

NA = 'na'
JP = 'jp'

"""
DB UPDATING/PROCESSING
"""


def _process_db():
    urllib.request.urlretrieve(DADGUIDEURL, DADGUIDEDB)
    db = DB(DADGUIDEDB)
    schema_to_region = {NA: 'en', JP: 'ja'}
    monsters = {NA: [], JP: []}
    for region, mons in monsters.items():
        series = {}
        for r in db.get_all('series', factory=True):
            series[r['series_id']] = r[f'name_{schema_to_region[region]}']
        for r in db.get_rows('monsters', (f'on_{region}', 1), factory=True):
            mons_id = r['monster_id']
            m = {
                'id': r[f'monster_no_{region}'],
                'name': r[f'name_{schema_to_region[region]}'],
                'hp_max': r['hp_max'],
                'atk_max': r['atk_max'],
                'rcv_max': r['rcv_max'],
                'cost': r['cost'],
                'exp': r['exp'],
                'rarity': r['rarity'],
                'lb_mult': r['limit_mult'],
                'attribute_1_id': r['attribute_1_id'],
                'attribute_2_id': r['attribute_2_id'],
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
            ls_row = db.get_row('leader_skills', ('leader_skill_id', ls_id), factory=True)
            if ls_row:
                m['leader'] = {
                    'leader_name': ls_row[f'name_{schema_to_region[region]}'],
                    'leader_desc': ls_row[f'desc_{schema_to_region[region]}'],
                    'max_hp': ls_row['max_hp'],
                    'max_atk': ls_row['max_atk'],
                    'max_rcv': ls_row['max_rcv'],
                    'max_shield': ls_row['max_shield']
                }

            as_id = r['active_skill_id']
            as_row = db.get_row('active_skills', ('active_skill_id', as_id), factory=True)
            if as_row:
                m['active'] = {
                    'active_name': as_row[f'name_{schema_to_region[region]}'],
                    'active_desc': as_row[f'desc_{schema_to_region[region]}'],
                    'turn_max': as_row['turn_max'],
                    'turn_min': as_row['turn_min'],
                }

            awakenings = db.get_rows('awakenings', ('monster_id', mons_id), factory=True)
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

            evos_from = db.get_rows('evolutions', ('from_id', mons_id), factory=True)
            evos_to = db.get_rows('evolutions', ('to_id', mons_id), factory=True)
            queue = [e['to_id'] for e in evos_from] + [e['from_id'] for e in evos_to]

            all_evos = []
            while queue:
                n = queue.pop(0)
                if not n in all_evos:
                    queue += [r['to_id'] for r in db.get_rows('evolutions', ('from_id', n), factory=True)]
                    queue += [r['from_id'] for r in db.get_rows('evolutions', ('to_id', n), factory=True)]
                    all_evos.append(n)
            if all_evos:
                all_evos.remove(mons_id)
            all_evos = [db.get_value('monsters', f'monster_no_{region}', ('monster_id', e)) for e in all_evos]
            m['evolutions'] = sorted(all_evos)

            mons.append(Monster(m))
    os.remove(DADGUIDEDB)
    return monsters


def update_monsters():
    all_monsters = _process_db()
    for region, monsters in all_monsters.items():
        db = DB(CARDS_TMP_DB % region)
        db.delete_table('monsters')
        db.create_table('monsters',
                        ('id', DB_MOD.INTEGER),
                        ('name', DB_MOD.TEXT),
                        ('att1', DB_MOD.TEXT),
                        ('att2', DB_MOD.TEXT),
                        ('series', DB_MOD.TEXT),
                        ('monster', DB_MOD.BLOB))
        for m in monsters:
            if 0 < m.id < 100000:
                db.insert_row('monsters',
                              ('id', m.id),
                              ('name', m.name),
                              ('att1', m.attribute_1_id),
                              ('att2', m.attribute_2_id),
                              ('series', m.series),
                              ('monster', mons_to_binary(m)))
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
            new_hp = abs(m.hp_max())
            new_atk = abs(m.atk_max())
            new_rcv = abs(m.rcv_max())
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
            db = DB(CARDS_DB % region)
            query, atts = parse_attribute(query.lower())
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
                name_split = m.name.lower().replace(',', '').replace('\'', '').split(' ')
                found = 0
                for s in split:
                    for n in name_split:
                        if s == n:
                            found += 1
                if found == len(split):
                    exact_names.append(m)
            return exact_names if exact_names else results
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
