window.addEventListener('load', function () {
    var deleteBtn = document.getElementById('confirmDeletion');
    deleteBtn.addEventListener('click', deleteItem);
})

/*function deleteItem(e){
        //console.log(1);
    if (confirm('Are you sure?')){
    } else {
        e.preventDefault();
    }
}*/

// Remove item
function deleteItem(e){
  if(e.target.classList.contains('remove-pf')){
    if(confirm('All shares tied to this stock would be erased. Are you sure?')){
    }else {
        e.preventDefault();
    }
  }
}