package com.example.ali.audavaproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityAlbumsListView extends ActionBarActivity {

    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_GALLERY = 1;

    private DataManager data;
    private AlbumItem selectedAlbum;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_list_view);

        data = new DataManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        data.removeDeletedAlbums();
        data.checkAlbumPictures();
        populateListView();
    }

    private void populateListView() {
        ArrayAdapter<AlbumItem> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.albumListView);
        list.setAdapter(adapter);
        registerForContextMenu(list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_album_page, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.add_album:
                addAlbum();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
                return true;
            case android.R.id.home:
                startActivity(new Intent(getApplicationContext(), ActivityRecord.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        menu.add(0, v.getId(), 0, "Rename");
        menu.add(0, v.getId(), 0, "Set Description");
        menu.add(0, v.getId(), 0, "Change album picture");
        menu.add(0, v.getId(), 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle()=="Rename") {
            if(selectedAlbum.getName().equals("Default Story Album")) {
                Toast.makeText(ActivityAlbumsListView.this, "Default Story Album cannot be renamed",
                        Toast.LENGTH_SHORT).show();
            } else {
                renameDialog();
            }
        }
        if(item.getTitle()=="Set Description") {
            descriptionDialog();
        }
        if(item.getTitle()=="Change album picture") {
            pictureDialog();
        }
        if(item.getTitle()=="Delete"){
            if(selectedAlbum.getName().equals("Default Story Album")) {
                Toast.makeText(ActivityAlbumsListView.this, "Default Story Album cannot be deleted",
                        Toast.LENGTH_SHORT).show();
            } else {
                deleteDialog();
            }
        }
        return false;
    }

    private void addAlbum(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Add album");

        LayoutInflater inflater = this.getLayoutInflater();
        View inputDialogView = inflater.inflate(R.layout.album_input_dialog, null);
        dialogBuilder.setView(inputDialogView);

        final EditText textInput = (EditText) inputDialogView.findViewById(R.id.albumName);
        final EditText descInput = (EditText) inputDialogView.findViewById(R.id.description);

        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String albumName = textInput.getText().toString().trim();
                if(!data.addAlbum(albumName, descInput.getText().toString())) {
                    Toast.makeText(getApplicationContext(),
                            "ERROR: Album with same name already exists", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void renameDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final EditText textInput = new EditText(this);
        textInput.setText(selectedAlbum.getName());
        textInput.setSelectAllOnFocus(true);

        textInput.requestFocus();
        textInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textInput, 0);
            }
        }, 100);

        dialogBuilder.setTitle("Rename album");
        dialogBuilder.setView(textInput);
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String rename = textInput.getText().toString().trim();
                if(data.renameAlbum(selectedAlbum, rename)) {
                    Toast.makeText(ActivityAlbumsListView.this, "Name changed to " + rename,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "ERROR: Album with same name already exists", Toast.LENGTH_SHORT)
                            .show();
                }
                populateListView();
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void descriptionDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final EditText textInput = new EditText(this);
        textInput.setText(selectedAlbum.getDescription());
        textInput.setSelectAllOnFocus(true);

        textInput.requestFocus();
        textInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textInput, 0);
            }
        }, 100);

        dialogBuilder.setTitle("Set Description (Max 3 Lines!)");
        dialogBuilder.setView(textInput);
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String description = textInput.getText().toString();
                data.changeAlbumDescription(selectedAlbum, description);
                Toast.makeText(ActivityAlbumsListView.this, "Description changed!" ,
                        Toast.LENGTH_SHORT).show();
                populateListView();
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void pictureDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setItems(new CharSequence[]{"Camera", "Gallery", "Remove Picture", "Cancel"},
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Toast.makeText(ActivityAlbumsListView.this, "Camera Selected",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        //File f = new File(android.os.Environment.getExternalStorageDirectory(),
                        //      "temp.jpg");
                        //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                        startActivityForResult(intent, REQUEST_CAMERA);
                        break;
                    case 1:
                        Toast.makeText(ActivityAlbumsListView.this, "Gallery Selected",
                                Toast.LENGTH_SHORT).show();
                        Intent intent2 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.
                                Images.Media.EXTERNAL_CONTENT_URI);
                        intent2.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent2, "Select File"),
                                REQUEST_GALLERY);
                        break;
                    case 2:
                        data.deleteAlbumPicture(selectedAlbum);
                        populateListView();
                        Toast.makeText(ActivityAlbumsListView.this, "Picture removed",
                                Toast.LENGTH_SHORT).show();
                    case 3:
                        break;
                }
            }
        });

        AlertDialog dialogPicture = dialogBuilder.create();
        dialogPicture.show();
    }

    private void deleteDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Delete Album and all it's content?");
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.removeAlbum(selectedAlbum);
                Toast.makeText(ActivityAlbumsListView.this, "Album " + selectedAlbum.getName() +
                        " Deleted", Toast.LENGTH_SHORT).show();
                populateListView();
            }
        });

        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogDelete = dialogBuilder.create();
        dialogDelete.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        if(requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            Uri ImageUri = intentData.getData();
            String imagePath = getPath(ImageUri, ActivityAlbumsListView.this);
            data.setAlbumPicture(selectedAlbum, imagePath);
            populateListView();
        }
        if(requestCode == REQUEST_GALLERY && resultCode == RESULT_OK ) {
            Uri selectedImageUri = intentData.getData();
            String imagePath = getPath(selectedImageUri, ActivityAlbumsListView.this);
            data.setAlbumPicture(selectedAlbum, imagePath);
            populateListView();
        }
    }

    private String getPath(Uri selectedImageUri, ActivityAlbumsListView albums) {
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor cursor = albums.managedQuery(selectedImageUri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private class MyListAdapter extends ArrayAdapter<AlbumItem> {
        public MyListAdapter() {
            super(ActivityAlbumsListView.this, R.layout.album_item_view, data.getListAlbums());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Make sure we have a view to work with
            View itemView = convertView;
            if (itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.album_item_view, parent, false);
            }

            //Find the album to work with
            final AlbumItem currentAlbum = data.getListAlbums().get(position);

            //Fill the image view
            ImageView imageView = (ImageView) itemView.findViewById(R.id.album_imageView);
            if(currentAlbum.getIconPicture() == null) {
                imageView.setImageResource(R.drawable.story_album); //default image
            } else {
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                Bitmap picture = BitmapFactory.decodeFile(currentAlbum.getIconPicture(),
                        bitmapOptions);
                imageView.setImageBitmap(picture);
            }

            // On click listener when user click on the album
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setCurrentAlbum(currentAlbum.getName());
                    startActivity(new Intent(getApplicationContext(), ActivityTracksListView.class));
                    String message = "You selected " + currentAlbum.getName();
                    Toast.makeText(ActivityAlbumsListView.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            imageView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selectedAlbum = currentAlbum;
                    return false;
                }
            });

            //Make
            final TextView makeText = (TextView) itemView.findViewById(R.id.textView);
            makeText.setText(currentAlbum.getName());
            // On click listener when user click on the album
            makeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setCurrentAlbum(currentAlbum.getName());
                    startActivity(new Intent(getApplicationContext(), ActivityTracksListView.class));
                    String message = "You selected " + currentAlbum.getName();
                    Toast.makeText(ActivityAlbumsListView.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            makeText.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selectedAlbum = currentAlbum;
                    return false;
                }
            });

            //Description
            final TextView makeDescription = (TextView) itemView.findViewById(R.id.album_description);
            makeDescription.setText(currentAlbum.getDescription());
            // On click listener when user click on the album
            makeDescription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setCurrentAlbum(currentAlbum.getName());
                    startActivity(new Intent(getApplicationContext(), ActivityTracksListView.class));
                    String message = "You selected " + currentAlbum.getName();
                    Toast.makeText(ActivityAlbumsListView.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            makeDescription.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selectedAlbum = currentAlbum;
                    return false;
                }
            });

            return itemView;
        }

    }
}