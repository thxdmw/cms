import { shortDate } from '../format.js';

export default {
    props: { item: { type: Object, required: true } },
    setup() {
        return { shortDate };
    },
    template: `
    <div class="blogs hover-shadow" style="border-radius: 12px;">
        <span v-if="item.top===1" class="fa fa-superscript-top superscript superscript-top"></span>
        <span class="blogpic">
            <a><img :src="item.coverImage"></a>
            <span class="blog-type">
                <router-link :to="'/blog/category/' + item.categoryId">{{ item.bizCategory ? item.bizCategory.name : '' }}</router-link>
            </span>
        </span>
        <h3 class="blogtitle"><router-link :to="'/blog/article/' + item.id">{{ item.title }}</router-link></h3>
        <p class="blogtext">{{ item.description }}</p>
        <div class="bloginfo">
            <ul>
                <li><span class="fa fa-clock-o"></span><span>{{ shortDate(item.createTime) }}</span></li>
                <li><span class="fa fa-eye"></span><span>{{ item.lookCount || 0 }}</span></li>
                <li>
                    <router-link class="comment-link-a" :to="'/blog/article/' + item.id + '#comment'">
                        <span class="fa fa-comments-o"></span><span>{{ item.commentCount || 0 }}</span>
                    </router-link>
                </li>
            </ul>
            <router-link class="read-more" :to="'/blog/article/' + item.id">阅读全文</router-link>
        </div>
    </div>
    `
};
