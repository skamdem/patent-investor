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
  if(e.target.classList.contains('deletion')){
    var isChecked = document.querySelectorAll('input[type="checkbox"]:checked').length === 0 ? false : true;
    if (!isChecked){
        alert('You did not check anything!');
        e.preventDefault();
    } else {
        if(confirm('Are You Sure?')){
        }else {
            e.preventDefault();
        }
    }
  }
}