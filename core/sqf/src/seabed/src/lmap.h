//------------------------------------------------------------------
//
// @@@ START COPYRIGHT @@@
//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
// @@@ END COPYRIGHT @@@

//
// Implement map and thread-safe map
//

#ifndef __SB_LMAP_H_
#define __SB_LMAP_H_

#include "seabed/int/opts.h"

#include "seabed/thread.h"

#include "mapcom.h"
#include "ml.h"

class SB_Lmap_Enum;

const float SB_LMAP_DEFAULT_LF = 0.75;

//
// This is an 'long' (id is long) map.
//
// Notes:
//   An item is an SB_LML_Type.
//   Typically this would be embedded within some data structure.
//   This type of construction makes get's and put's very fast.
//
// constructors:
//   pp_name   : name of map
//   pv_buckets: initial number of buckets
//   pv_lf     : load factor
//
// methods:
//   empty     : returns true if map is empty
//   get       : returns item given a id
//   keys      : returns enumeration of items
//   printself : prints map
//   put       : add item to map
//   remove    : remove item from map
//   removeall : remove all items from map
//   resize    : set new number of buckets
//   size      : returns number of items in map
//
class SB_Lmap : public SB_Map_Comm {
public:
    SB_Lmap(int   pv_buckets = DEFAULT_BUCKETS,
            float pv_lf = SB_LMAP_DEFAULT_LF);
    SB_Lmap(const char *pp_name,
            int         pv_buckets = DEFAULT_BUCKETS,
            float       pv_lf = SB_LMAP_DEFAULT_LF);
    virtual ~SB_Lmap();

    friend class SB_Lmap_Enum;
    typedef long Key_Type;

    bool                  empty();
    virtual void         *get(Key_Type pv_id);
    virtual SB_Lmap_Enum *keys();
    virtual void          printself(bool pv_traverse);
    virtual void          put(SB_LML_Type *pp_item);
    virtual void         *remove(Key_Type pv_id);
    virtual void          removeall();
    virtual void          resize(int pv_buckets);
    int                   size();

protected:
#ifdef LMAP_CHECK
    void                  check_integrity();
#endif // LMAP_CHECK
    int                   hash(Key_Type pv_id, int pv_buckets);
    void                  init();

    enum        { DEFAULT_BUCKETS = 61 };
    SB_LML_Type **ipp_HT;
};

class SB_Ts_Lmap : public SB_Lmap {
public:
    SB_Ts_Lmap(int   pv_buckets = DEFAULT_BUCKETS,
               float pv_lf = SB_LMAP_DEFAULT_LF)
    : SB_Lmap(pv_buckets, pv_lf) {}
    SB_Ts_Lmap(const char *pp_name,
               int         pv_buckets = DEFAULT_BUCKETS,
               float       pv_lf = SB_LMAP_DEFAULT_LF)
    : SB_Lmap(pp_name, pv_buckets, pv_lf) { iv_lock.setname(pp_name); }
    ~SB_Ts_Lmap() {}

    virtual void *get(Key_Type pv_id);
    virtual void *get_lock(Key_Type pv_id, bool pv_lock);
    virtual void  lock();
    virtual void  printself(bool pv_traverse);
    virtual void  put(SB_LML_Type *pp_item);
    virtual void  put_lock(SB_LML_Type *pp_item, bool pv_lock);
    virtual void *remove(Key_Type pv_id);
    virtual void *remove_lock(Key_Type pv_id, bool pv_lock);
    virtual void  resize(int pv_buckets);
    void          setlockname(const char *pp_lockname);
    virtual int   trylock();
    virtual void  unlock();

private:
    SB_Thread::MSL   iv_lock;         // for protection
};

class SB_Lmap_Enum {
public:
    SB_Lmap_Enum(SB_Lmap *pp_map);
    virtual ~SB_Lmap_Enum() {}

    virtual bool         more();
    virtual SB_LML_Type *next();

protected:
    SB_Lmap_Enum() {}

private:
    SB_LML_Type *ip_item;
    SB_Lmap     *ip_map;
    int          iv_count;
    int          iv_hash;
    int          iv_inx;
    int          iv_mod;
};

#ifdef USE_SB_INLINE
#include "lmap.inl"
#endif

#endif // !__SB_LMAP_H_
