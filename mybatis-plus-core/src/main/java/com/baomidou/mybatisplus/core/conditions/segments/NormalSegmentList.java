/*
 * Copyright (c) 2011-2022, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.core.conditions.segments;

import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 普通片段
 *
 * @author miemie
 * @since 2018-06-27
 */
@SuppressWarnings("serial")
public class NormalSegmentList extends AbstractISegmentList {

    /**
     * 是否处理了的上个 not
     */
    private boolean executeNot = true;

    NormalSegmentList() {
        this.flushLastValue = true;
    }

    @Override
    protected boolean transformList(List<ISqlSegment> list, ISqlSegment firstSegment, ISqlSegment lastSegment) {
        if (list.size() == 1) {
            /* 只有 and() 以及 or() 以及 not() 会进入 */
            // 只有and() 或者 or 或者 not进入这里的方法
            if (!MatchSegment.NOT.match(firstSegment)) {
                //不是 not
                // 如果是第一次进入这里的话，那么代表用户直接使用了not，或者or或者and，不加入到集合中
                if (isEmpty()) {
                    //sqlSegment是 and 或者 or 并且在第一位,不继续执行
                    return false;
                }

                boolean matchLastAnd = MatchSegment.AND.match(lastValue);
                boolean matchLastOr = MatchSegment.OR.match(lastValue);
                // 判断上一次是否是and或者or
                if (matchLastAnd || matchLastOr) {
                    //上次最后一个值是 and 或者 or
                    if (matchLastAnd && MatchSegment.AND.match(firstSegment)) {
                        return false;
                    } else if (matchLastOr && MatchSegment.OR.match(firstSegment)) {
                        return false;
                    } else {
                        //和上次的不一样
                        // 进入到这里说明上一次是and，这次是or
                        // 或者上一次是or，这次是and
                        // 删除最后上一次的值，使用最新的值
                        removeAndFlushLast();
                    }
                }
            } else {
                // 代表这次是not了
                // 等到下次的时候直接添加到集合里面
                executeNot = false;
                return false;
            }
        } else {
            // 判断是否是普通的添加
            if (MatchSegment.APPLY.match(firstSegment)) {
                list.remove(0);
            }
            // 如果上一次既不是and，也不是or，而且集合里面有值，
            // 那么添加上默认的and
            if (!MatchSegment.AND_OR.match(lastValue) && !isEmpty()) {
                add(SqlKeyword.AND);
            }

            // 代表上一次是not，这里添加上not符号
            if (!executeNot) {
                list.add(0, SqlKeyword.NOT);
                executeNot = true;
            }
        }
        return true;
    }

    @Override
    protected String childrenSqlSegment() {
        if (MatchSegment.AND_OR.match(lastValue)) {
            removeAndFlushLast();
        }
        final String str = this.stream().map(ISqlSegment::getSqlSegment).collect(Collectors.joining(SPACE));
        return (LEFT_BRACKET + str + RIGHT_BRACKET);
    }

    @Override
    public void clear() {
        super.clear();
        flushLastValue = true;
        executeNot = true;
    }
}
