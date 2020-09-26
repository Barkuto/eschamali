import sqlite3
import os
import os.path

COL_TYPES = ['text', 'integer', 'blob']
TEXT = STRING = COL_TYPES[0]
INTEGER = INT = COL_TYPES[1]
BLOB = COL_TYPES[2]


def __sanitize(data):
    if type(data) is str:
        for ch in ['(', ')', '[', ']', ';', '-']:
            if ch in data:
                data = data.replace(ch, '')
    elif type(data) is tuple:
        new_data = ()
        for t in data:
            new_data = new_data + (__sanitize(t),)
        data = new_data
    return data


def _sanitize(*args):
    return __sanitize(args)


class DB():
    def __init__(self, db_path):
        db_path = str(db_path)
        try:
            os.makedirs(os.path.dirname(db_path), exist_ok=True)
        except FileNotFoundError:
            pass
        if db_path.endswith('.db') or db_path.endswith('.sqlite'):
            self.db_path = db_path
        else:
            self.db_path = db_path + '.db'

    def create_table(self, table_name, *columns):
        table_name, columns = _sanitize(table_name, columns)
        if not columns:
            return
        cols = ''
        for col in columns:
            cols = cols + f'{col[0]} {col[1]},'
        try:
            conn = sqlite3.connect(self.db_path)
            conn.execute(f'CREATE TABLE IF NOT EXISTS {table_name} ({cols.rstrip(",")})')
            return True
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def delete_table(self, table_name):
        table_name, = _sanitize(table_name)
        try:
            conn = sqlite3.connect(self.db_path)
            conn.execute(f'DROP TABLE IF EXISTS {table_name}')
            return True
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def table_exists(self, table_name):
        table_name, = _sanitize(table_name)
        try:
            conn = sqlite3.connect(self.db_path)
            result = conn.execute('SELECT name FROM sqlite_master WHERE type=\'table\' AND name=?',
                                  (table_name,)).fetchone()
            return True if result else False
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def insert_row(self, table_name, *columns):
        table_name, columns = _sanitize(table_name, columns)
        try:
            conn = sqlite3.connect(self.db_path)
            conn.execute('INSERT INTO %s (%s) VALUES (%s)' %
                         (table_name,
                          ','.join([c[0] for c in columns]),
                          ','.join((['?']*len(columns)))),
                         tuple([c[1] for c in columns]))
            conn.commit()
            return True
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def delete_rows(self, table_name, *match_cols):
        table_name, match_cols = _sanitize(table_name, match_cols)
        try:
            conn = sqlite3.connect(self.db_path)
            conn.execute('DELETE FROM %s WHERE %s' %
                         (table_name,
                          ' AND '.join([f'{c[0]}=?' for c in match_cols])),
                         tuple([c[1] for c in match_cols]))
            conn.commit()
            return True
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def update_row(self, table_name, *columns):
        table_name, columns = _sanitize(table_name, columns)
        try:
            conn = sqlite3.connect(self.db_path)
            conn.execute('UPDATE {} SET {} WHERE {}=?'.format(
                table_name,
                ','.join(['{}=\'{}\''.format(c[0], c[1]) for c in columns[1:]]),
                columns[0][0]), (columns[0][1],))
            conn.commit()
            return True
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return False

    def _get_rows(self, table_name, *match_cols, factory=False, num='ALL'):
        table_name, match_cols = _sanitize(table_name, match_cols)
        try:
            conn = sqlite3.connect(self.db_path)
            if factory:
                conn.row_factory = sqlite3.Row
            c = conn.execute('SELECT * FROM %s WHERE %s' %
                             (table_name,
                              ' AND '.join([f'{c[0]}=?' for c in match_cols])),
                             tuple([c[1] for c in match_cols]))
            return c.fetchall() if num == 'ALL' else c.fetchone()
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return [] if num == 'ALL' else None

    def get_rows(self, table_name, *match_cols, factory=False):
        return self._get_rows(table_name, *match_cols, factory=factory, num='ALL')

    def get_row(self, table_name, *match_cols, factory=False):
        return self._get_rows(table_name, *match_cols, factory=factory, num='ONE')

    def get_rows_like(self, table_name, match_cols=(), like_cols=(), factory=False):
        table_name, match_cols, like_cols = _sanitize(table_name, match_cols, like_cols)
        try:
            conn = sqlite3.connect(self.db_path)
            if factory:
                conn.row_factory = sqlite3.Row
            query = f'SELECT * FROM {table_name} WHERE %s'
            where = ' AND '.join([f'{c[0]}=?' if c[1] else f'{c[0]} is ?' for c in match_cols])
            like = ' AND '.join([f'({c[0]} LIKE ?)' for c in like_cols])
            where_values = tuple([c[1] for c in match_cols])
            like_values = tuple([f'%{c[1]}%' for c in like_cols])
            c = conn.execute(query % (where + f' AND {like}' if like and where else like if like else ''),
                             where_values + like_values)
            return c.fetchall()
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return [-1]

    def get_value(self, table_name, value, match_col):
        table_name, value, match_col = _sanitize(table_name, value, match_col)
        result = None
        try:
            conn = sqlite3.connect(self.db_path)
            c = conn.execute(f'SELECT {value} FROM {table_name} WHERE {match_col[0]}=?', (match_col[1],))
            result = c.fetchone()
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return result[0] if result else None

    def get_values(self, table_name, value, match_col):
        table_name, value, match_col = _sanitize(table_name, value, match_col)
        result = []
        try:
            conn = sqlite3.connect(self.db_path)
            c = conn.execute(f'SELECT {value} FROM {table_name} WHERE {match_col[0]}=?', (match_col[1],))
            result = [r[0] for r in c.fetchall()]
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return result

    def get_all(self, table_name, factory=False):
        table_name, = _sanitize(table_name)
        result = []
        try:
            conn = sqlite3.connect(self.db_path)
            if factory:
                conn.row_factory = sqlite3.Row
            c = conn.execute(f'SELECT * FROM {table_name}')
            result = c.fetchall()
        except sqlite3.Error as e:
            raise e
        finally:
            conn.close()
        return result

    def add_to_col(self, table_name, match_col, new_col):
        old_val = self.get_value(table_name, new_col[0], match_col)
        new_val = '|'.join(str(old_val).split('|') + [str(new_col[1])])
        new_val = f'"{new_val}"'
        if old_val:
            self.update_row(table_name, match_col, (new_col[0], new_val))
