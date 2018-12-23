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
            this.$root.$emit('perform-faceted-search', facet, isAdd);
        }
    }
}