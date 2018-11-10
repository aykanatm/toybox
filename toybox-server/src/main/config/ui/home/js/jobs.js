const jobs = new Vue({
    el: '#toybox-jobs',
    data:{
        view: 'jobs',
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
        sortedDesByStatus: false
    },
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName);
        },
        getJobs(offset, limit, sortType, sortColumn, username)
        {
            this.getConfiguration("jobServiceUrl")
            .then(response => {
                return axios.get(response.data.value + "/jobs?offset=" + offset + "&limit=" + limit + "&sort_type=" + sortType + "&sort_column=" + sortColumn + "&username=" + username);
            })
            .then(response => {
                console.log(response);

                this.jobs = response.data.jobs;
                this.totalRecords = response.data.totalRecords;
                this.totalPages = Math.ceil(this.totalRecords / this.limit);
                this.currentPage = Math.ceil((offset / limit) + 1);
            })
            .then(response => {
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
                this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username);
            }
        },
        nextPage(){
            if(this.currentPage != this.totalPages){
                this.offset += this.limit;
                this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username);
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

            this.getJobs(this.defaultOffset, this.defaultLimit, this.sortType, this.sortColumn, this.username);
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
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        this.limit = this.defaultLimit;
        this.offset = this.defaultOffset;
        this.sortType = this.defaultSortType;
        this.sortColumn = this.defaultSortColumn;

        this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username);
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'job' : httpVueLoader('../components/job/job.vue')
    }
});