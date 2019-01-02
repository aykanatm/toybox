const jobs = new Vue({
    el: '#toybox-jobs',
    data:{
        view: 'jobs',
        isLoading: true,
        jobs:[],
        // TODO:
        // Make username dynamic
        username: 'test',
        // Pagination
        currentPage: 1,
        defaultLimit: 10,
        defaultOffset: 0,
        limit: 10,
        offset: 0,
        totalRecords: 0,
        totalPages: 0,
        previousPageButtonDisabled: true,
        nextPageButtonDisabled: false,
        startIndex: 0,
        endIndex: 0,
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
        // Filtering
        facets: [],
        jobSearchRequestFacetList: [],
        // Messages
        messages: [],
    },
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName)
                .catch(error => {
                    var errorMessage;

                    if(error.response){
                        errorMessage = error.response.data.message
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    this.displayMessage('Error', errorMessage);
                });
        },
        getJobs(offset, limit, sortType, sortColumn, username, jobSearchRequestFacetList)
        {
            this.getConfiguration("jobServiceUrl")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.sortType = sortType;
                    searchRequest.sortColumn = sortColumn;
                    searchRequest.username = username;
                    searchRequest.jobSearchRequestFacetList = jobSearchRequestFacetList;
                    return axios.post(response.data.value + "/jobs/search", searchRequest)
                        .catch(error => {
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
                                }
                                else{
                                    errorMessage = error.message;
                                }

                                console.error(errorMessage);
                                this.displayMessage('Error', errorMessage);
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
                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }
                    else{
                        this.displayMessage('Information','There is no job in the system');
                    }
                }
            })
            .then(response => {
                console.log(response);
                this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                this.updateSortStatus(this.sortType, this.sortColumn)
            });
        },
        // Pagination
        updatePagination(currentPage, totalPages, offset, limit, totalRecords){
            this.startIndex = offset + 1;
            this.endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

            if(currentPage == 1){
                this.nextPageButtonDisabled = false;
                this.previousPageButtonDisabled = true;
            }
            else if(currentPage == totalPages){
                this.nextPageButtonDisabled = true;
                this.previousPageButtonDisabled = false;
            }
            else{
                this.nextPageButtonDisabled = false;
                this.previousPageButtonDisabled = false;
            }
        },
        previousPage(){
            if(this.currentPage != 1){
                this.offset -= this.limit;
                this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
            }
        },
        nextPage(){
            if(this.currentPage != this.totalPages){
                this.offset += this.limit;
                this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
            }
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

            this.getJobs(this.defaultOffset, this.defaultLimit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
        },
        displayMessage(type, messageString){
            var isError = false;
            var isOk = false;
            var isWarning = false;
            var isInfo = false;

            if(type === 'Information'){
                isInfo = true;
            }
            else if(type === 'Error'){
                isError = true;
            }
            else if(type === 'Warning'){
                isWarning = true;
            }
            else if(type === 'Success'){
                isOk = true;
            }
            else{
                isInfo = true;
            }

            var message = {id:this.messages.length, isError:isError, isOk:isOk, isWarning:isWarning, isInfo:isInfo, header:type, message:messageString}
            this.messages.push(message);
            // Initialize events on the message
            setTimeout(() => {
                $('.message .close')
                    .on('click', function() {
                        $(this)
                        .closest('.message')
                        .remove();
                    });

                var messageSelector = '#' + message.id + '.message';
                $(messageSelector)
                    .delay(2000)
                    .queue(function(){
                        $(this).remove().dequeue();
                    });
            }, 100);
        }
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

        this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);

        // Initialize event listeners
        this.$root.$on('perform-faceted-search', (facet, isAdd) => {
            if(isAdd){
                console.log('Adding facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' to search');
                this.jobSearchRequestFacetList.push(facet);
            }
            else{
                console.log('Removing facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' from search');
                var index;
                for(var i = 0; i < this.jobSearchRequestFacetList.length; i++){
                    var jobRequestFacet = this.jobSearchRequestFacetList[i];
                    if(jobRequestFacet.fieldName === facet.fieldName && jobRequestFacet.fieldValue === facet.fieldValue){
                        index = i;
                        break;
                    }
                }
                this.jobSearchRequestFacetList.splice(index, 1);
            }

            this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
        });

        this.$root.$on('message-sent', this.displayMessage);
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'job' : httpVueLoader('../components/job/job.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'message' : httpVueLoader('../components/message/message.vue')
    }
});