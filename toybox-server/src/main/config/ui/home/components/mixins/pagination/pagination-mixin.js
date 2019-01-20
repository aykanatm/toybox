var paginationMixin = {
    data:function(){
        return{
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
        }
    },
    methods:{
        updatePagination:function(currentPage, totalPages, offset, limit, totalRecords){
            this.startIndex = offset + 1;
            this.endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

            if(currentPage == 1){
                if(totalPages != 1){
                    this.nextPageButtonDisabled = false;
                    this.previousPageButtonDisabled = true;
                }
                else{
                    this.nextPageButtonDisabled = true;
                    this.previousPageButtonDisabled = true;
                }
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
                if(this.view === 'jobs'){
                    this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
                }
                else if(this.view === 'files'){
                    this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.assetSearchRequestFacetList);
                }
            }
        },
        nextPage(){
            if(this.currentPage != this.totalPages){
                this.offset += this.limit;
                if(this.view === 'jobs'){
                    this.getJobs(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.jobSearchRequestFacetList);
                }
                else if(this.view === 'files'){
                    this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.assetSearchRequestFacetList);
                }
            }
        },
    }
}