const folders = new Vue({
    el: '#toybox-jobs',
    data:{
        view: 'jobs',
        jobs:[],
        currentPage: 1,
        limit: 10,
        offset: 0,
        totalRecords: 0,
        totalPages: 0,
        previousPageButtonDisabled: true,
        nextPageButtonDisabled: false,
        startIndex: 0,
        endIndex: 0
    },
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName);
        },
        getJobs(offset, limit)
        {
            this.getConfiguration("jobServiceUrl")
            .then(response => {
                return axios.get(response.data.value + "/jobs?offset=" + offset + "&limit=" + limit);
            })
            .then(response => {
                console.log(response);

                this.jobs = response.data.jobs;
                this.totalRecords = response.data.totalRecords;
                this.totalPages = Math.ceil(this.totalRecords / this.limit);
                this.currentPage = Math.ceil((offset / limit) + 1);
            })
            .then(response =>{
                this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
            });
        },
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
                this.getJobs(this.offset, this.limit);
            }
        },
        nextPage(){
            if(this.currentPage != this.totalPages){
                this.offset += this.limit;
                this.getJobs(this.offset, this.limit);
            }
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

        this.getJobs(this.offset, this.limit);
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'job' : httpVueLoader('../components/job/job.vue')
    }
});