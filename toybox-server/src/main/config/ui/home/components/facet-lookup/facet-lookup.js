module.exports = {
    props:{
        value: String
    },
    methods:{
        click:function(event){
            console.log(event);
            var input = event.path[0];
            var fieldValue = event.path[1].textContent.trim();
            var fieldName = event.path[4].firstChild.innerText.trim();
            var isAdd = input.checked;
            var facet = {fieldName: fieldName, fieldValue: fieldValue};
            // TODO: Find a better way to do this, add a type value to facets
            var isDate = fieldName.toLowerCase().includes('time') || fieldName.toLowerCase().includes('date');
            this.$root.$emit('perform-faceted-search', facet, isAdd, isDate);
        }
    }
}