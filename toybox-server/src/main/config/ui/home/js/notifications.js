const files = new Vue({
    el: '#toybox-notifications',
    mixins:[paginationMixin, messageMixin, facetMixin, userMixin, serviceMixin, notificationMixin],
    data:{
        view: 'notifications',
        notifications:[],
        fromUsername: null,
        content: '*',
        notificationDate: new Date(),
        isRead: null,
        isLoading: false,
        // Overrides
        limit: 10
    },
    mounted:function(){
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var csrfToken = $("meta[name='_csrf']").attr("content");
        console.log('CSRF header: ' + csrfHeader);
        console.log('CSRF token: ' + csrfToken);
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        // Initialize accordions
        $('.ui.accordion').accordion();

        // Initialize event listeners
        this.$root.$on('perform-faceted-search', (facet, isAdd) => {
            if(isAdd){
                console.log('Adding facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' to search');
                this.searchRequestFacetList.push(facet);
            }
            else{
                console.log('Removing facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' from search');
                var index;
                for(var i = 0; i < this.searchRequestFacetList.length; i++){
                    var searchRequestFacet = this.searchRequestFacetList[i];
                    if(searchRequestFacet.fieldName === facet.fieldName && searchRequestFacet.fieldValue === facet.fieldValue){
                        index = i;
                        break;
                    }
                }
                this.searchRequestFacetList.splice(index, 1);
            }

            this.getNotifications(this.fromUsername, this.content, this.notificationDate, this.isRead, this.offset, this.limit, this.searchRequestFacetList);
        });

        this.$root.$on('message-sent', this.displayMessage);
        this.$root.$on('refresh-notifications', this.refreshNotifications);

        this.getNotifications(this.fromUsername, this.content, this.notificationDate, this.isRead, this.offset, this.limit, this.searchRequestFacetList);
    },
    methods:{
        refreshNotifications:function(){
            this.getNotifications(this.fromUsername, this.content, this.notificationDate, this.isRead, this.offset, this.limit, this.searchRequestFacetList);
        }
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'notification' : httpVueLoader('../components/notification/notification.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'message' : httpVueLoader('../components/message/message.vue'),
    }
});