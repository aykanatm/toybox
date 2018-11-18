module.exports = {
    props:{
        id: Number,
        name: String,
        lookups: Array
    },
    components:{
        'facet-lookup' : httpVueLoader('../facet-lookup/facet-lookup.vue')
    }
}