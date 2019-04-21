const jobs = new Vue({
    el: '#toybox-jobs',
    mixins:[paginationMixin, messageMixin, facetMixin, userMixin, serviceMixin],
    data:{
        view: 'jobs',
        isLoading: true,
        jobs:[],
        // Sorting
        defaultSortType: 'des',
        defaultSortColumn: 'END_TIME',
        sortType: 'des',
        sortColumn: 'END_TIME',
        sortedAscByJobName: false,
        sortedDesByJobName: false,
        sortedAscByJobType: false,
        sortedDesByJobType: false,
        sortedAscByStartTime: false,
        sortedDesByStartTime: false,
        sortedAscByEndTime: false,
        sortedDesByEndTime: false,
        sortedAscByStatus: false,
        sortedDesByStatus: false,
    },
    methods:{
        getJobs(offset, limit, sortType, sortColumn, searchRequestFacetList)
        {
            this.getService("toybox-job-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.sortType = sortType;
                    searchRequest.sortColumn = sortColumn;
                    searchRequest.jobSearchRequestFacetList = searchRequestFacetList;
                    return axios.post(response.data.value + "/jobs/search", searchRequest)
                        .catch(error => {
                                var errorMessage;

                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else{
                                    if(error.response){
                                        errorMessage = error.response.data.message
                                    }
                                    else{
                                        errorMessage = error.message;
                                    }

                                    console.error(errorMessage);
                                    this.displayMessage('Error', errorMessage);
                                }
                            });
                }
            })
            .then(response => {
                console.log(response);
                if(response){
                    this.isLoading = false;
                    if(response.status != 204){
                        this.jobs = response.data.jobs;
                        this.facets = response.data.facets;
                        this.totalRecords = response.data.totalRecords;
                        this.totalPages = Math.ceil(this.totalRecords / limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }
                    else{
                        this.displayMessage('Information','There is no job in the system');
                    }
                }
            })
            .then(response => {
                console.log(response);
                this.updatePagination(this.currentPage, this.totalPages, this.offset, this.limit, this.totalRecords);
                this.updateSortStatus(this.sortType, this.sortColumn)
            });
        },
        // Sorting
        resetSorting(){
            this.sortedAscByJobName = false;
            this.sortedDesByJobName = false;
            this.sortedAscByJobType = false;
            this.sortedDesByJobType = false;
            this.sortedAscByStartTime = false;
            this.sortedDesByStartTime = false;
            this.sortedAscByEndTime = false;
            this.sortedDesByEndTime = false;
            this.sortedAscByStatus = false;
            this.sortedDesByStatus = false;
        },
        updateSortStatus(sortType, sortColumn){
            this.resetSorting();
            if(sortColumn === 'JOB_NAME'){
                if(sortType === 'asc'){
                    this.sortedAscByJobName = true;
                    this.sortedDesByJobName = false;
                }
                else if(sortType === 'des'){
                    this.sortedAscByJobName = false;
                    this.sortedDesByJobName = true;
                }
            }
            else if(sortColumn === 'JOB_TYPE'){
                if(sortType === 'asc'){
                    this.sortedAscByJobType = true;
                    this.sortedDesByJobType = false;
                }
                else if(sortType === 'des'){
                    this.sortedAscByJobType = false;
                    this.sortedDesByJobType = true;
                }
            }
            else if(sortColumn === 'START_TIME'){
                if(sortType === 'asc'){
                    this.sortedAscByStartTime = true;
                    this.sortedDesByStartTime = false;
                }
                else if(sortType === 'des'){
                    this.sortedAscByStartTime = false;
                    this.sortedDesByStartTime = true;
                }
            }
            else if(sortColumn === 'END_TIME'){
                if(sortType === 'asc'){
                    this.sortedAscByEndTime = true;
                    this.sortedDesByEndTime = false;
                }
                else if(sortType === 'des'){
                    this.sortedAscByEndTime = false;
                    this.sortedDesByEndTime = true;
                }
            }
            else if(sortColumn === 'STATUS'){
                if(sortType === 'asc'){
                    this.sortedAscByStatus = true;
                    this.sortedDesByStatus = false;
                }
                else if(sortType === 'des'){
                    this.sortedAscByStatus = false;
                    this.sortedDesByStatus = true;
                }
            }
        },
        sort(sortType, sortColumn){
            this.sortType = sortType;
            this.sortColumn = sortColumn;

            this.getJobs(this.defaultOffset, this.defaultLimit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
    },
    computed:{
        // Job name
        isJobNameSorted:function(){
            return this.sortedAscByJobName || this.sortedDesByJobName;
        },
        isJobNameSortedAsc:function(){
            return this.sortedAscByJobName && !this.sortedDesByJobName;
        },
        isJobNameSortedDes:function(){
            return this.sortedDesByJobName && !this.sortedAscByJobName;
        },
        // Job type
        isJobTypeSorted:function(){
            return this.sortedAscByJobType || this.sortedDesByJobType;
        },
        isJobTypeSortedAsc:function(){
            return this.sortedAscByJobType && !this.sortedDesByJobType;
        },
        isJobTypeSortedDes:function(){
            return this.sortedDesByJobType && !this.sortedAscByJobType;
        },
        // Start time
        isStartTimeSorted:function(){
            return this.sortedAscByStartTime || this.sortedDesByStartTime;
        },
        isStartTimeSortedAsc:function(){
            return this.sortedAscByStartTime && !this.sortedDesByStartTime;
        },
        isStartTimeSortedDes:function(){
            return this.sortedDesByStartTime && !this.sortedAscByStartTime;
        },
        // End time
        isEndTimeSorted:function(){
            return this.sortedAscByEndTime || this.sortedDesByEndTime;
        },
        isEndTimeSortedAsc:function(){
            return this.sortedAscByEndTime && !this.sortedDesByEndTime;
        },
        isEndTimeSortedDes:function(){
            return this.sortedDesByEndTime && !this.sortedAscByEndTime;
        },
        // Status
        isStatusSorted:function(){
            return this.sortedAscByStatus || this.sortedDesByStatus;
        },
        isStatusSortedAsc:function(){
            return this.sortedAscByStatus && !this.sortedDesByStatus;
        },
        isStatusSortedDes:function(){
            return this.sortedDesByStatus && !this.sortedAscByStatus;
        }
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

        this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);

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

            this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        });

        this.$root.$on('message-sent', this.displayMessage);
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'job' : httpVueLoader('../components/job/job.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'message' : httpVueLoader('../components/message/message.vue'),
        'job-details-modal-window' : httpVueLoader('../components/job-details-modal-window/job-details-modal-window.vue')
    }
});