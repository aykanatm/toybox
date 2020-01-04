const shares = new Vue({
    el: '#toybox-shares',
    mixins:[paginationMixin, messageMixin, facetMixin, userMixin, serviceMixin],
    data:{
        view: 'shares',
        // isLoading: true,
        isLoading: true,
        shares:[],
        defaultSortType: 'des',
        defaultSortColumn: 'creation_date',
    },
    computed:{

    },
    mounted:function(){
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var csrfToken = $("meta[name='_csrf']").attr("content");
        console.log('CSRF header: ' + csrfHeader);
        console.log('CSRF token: ' + csrfToken);

        // Set axios configuration
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        // Initialize accordions
        $('.ui.accordion').accordion();

        this.limit = this.defaultLimit;
        this.offset = this.defaultOffset;
        this.sortType = this.defaultSortType;
        this.sortColumn = this.defaultSortColumn;

        this.getShares(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);

        // Initialize event listeners
        this.$root.$on('perform-faceted-search', (facet, isAdd, isDate) => {
            if(isAdd){
                if(isDate){
                    var indexes = [];
                    for(var i = 0; i < this.searchRequestFacetList.length; i++){
                        var jobRequestFacet = this.searchRequestFacetList[i];
                        if(jobRequestFacet.fieldName === facet.fieldName){
                            indexes.push(i);
                        }
                    }

                    for(var i = 0; i < indexes.length; i++){
                        var index = indexes[i];
                        this.searchRequestFacetList.splice(index, 1);
                    }
                }
                console.log('Adding facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' to search');
                this.searchRequestFacetList.push(facet);
            }
            else{
                console.log('Removing facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' from search');
                var index;
                for(var i = 0; i < this.searchRequestFacetList.length; i++){
                    var jobRequestFacet = this.searchRequestFacetList[i];
                    if(jobRequestFacet.fieldName === facet.fieldName && jobRequestFacet.fieldValue === facet.fieldValue){
                        index = i;
                        break;
                    }
                }
                this.searchRequestFacetList.splice(index, 1);
            }

            this.getShares(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        });

        this.$root.$on('message-sent', this.displayMessage);
    },
    methods:{
        getShares(offset, limit, sortType, sortColumn, searchRequestFacetList){
            this.isLoading = true;
            this.getService("toybox-share-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.sortType = sortType;
                    searchRequest.sortColumn = sortColumn;
                    searchRequest.assetSearchRequestFacetList = searchRequestFacetList;

                    return axios.post(response.data.value + "/share/search", searchRequest)
                        .catch(error => {
                            this.isLoading = false;
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                            }
                            else{
                                errorMessage = error.message;
                            }

                            console.error(errorMessage);
                            this.$root.$emit('message-sent', 'Error', errorMessage);
                        });
                }
                else{
                    this.isLoading = false;
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            })
            .then(response => {
                if(response){
                    this.isLoading = false;

                    console.log(response);

                    this.shares = response.data.shares;
                    this.facets = response.data.facets;

                    if(response.status == 204){
                        this.displayMessage('Information','You do not have any files.');
                        this.totalRecords = 0;
                        this.totalPages = 0;
                        this.currentPage = 0;
                    }
                    else{
                        this.totalRecords = response.data.totalRecords;
                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }

                    this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                }
                else{
                    this.isLoading = false;
                    this.$root.$emit('message-sent', 'Error', "There was no response from the share loadbalancer!");
                }
            })
        }
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'message' : httpVueLoader('../components/message/message.vue'),
        'share' : httpVueLoader('../components/share/share.vue'),
        'share-modal-window': httpVueLoader('../components/share-modal-window/share-modal-window.vue'),
    }
});