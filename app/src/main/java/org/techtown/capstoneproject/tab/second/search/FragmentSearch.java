package org.techtown.capstoneproject.tab.second.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.json.JSONObject;
import org.techtown.capstoneproject.R;
import org.techtown.capstoneproject.service.api.ApiService;
import org.techtown.capstoneproject.service.api.ApiServiceChemical;
import org.techtown.capstoneproject.service.api.MyRetrofit2;
import org.techtown.capstoneproject.service.api.UploadService;
import org.techtown.capstoneproject.service.dto.TestDTO;
import org.techtown.capstoneproject.tab.second.search.result.modification.Modification;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.app.Activity.RESULT_OK;

/*
 * Created by ShimPiggy on 2018-05-07.
 * Modified by ShimPiggy on 2018-05-09. - Camera
 * Modified by ShimPiggy on 2018-05-19. - modify changed design and control
 * Modified by ShimPiggy on 2018-05-23. - image
 */

public class FragmentSearch extends Fragment implements View.OnClickListener {
    private static final int PICK_FROM_CAMERA = 0;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 1818;

    private Uri mImageCaptureUri;
    private String fileName;

    private ImageButton btnName;
    private ImageButton btnDetail;
    private ImageButton btnGallery;
    private ImageButton btnWrite;

    private Retrofit retrofit;
    private ApiServiceChemical apiService_chemical;
    static ArrayList<TestDTO> arrayList;

    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public FragmentSearch() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        verifyStoragePermissions(getActivity());

        Init(view);

        getChemicalNameList();

        return view;
    }

    //persmission method.
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    public void Init(View view) {
        btnName = (ImageButton) view.findViewById(R.id.btn_name);//제품명
        btnDetail = (ImageButton) view.findViewById(R.id.btn_detail);//화학성분
        btnGallery = (ImageButton) view.findViewById(R.id.btn_gallery);//바코드
        btnWrite = (ImageButton) view.findViewById(R.id.btn_write);//직접 쓰기

        btnGallery.setOnClickListener(this);
        btnName.setOnClickListener(this);
        btnDetail.setOnClickListener(this);
        btnWrite.setOnClickListener(this);

        //Test
        arrayList = new ArrayList<>();
    }//init

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_detail:
                ButtonDetailListener(v);
                break;
            case R.id.btn_name:
                ButtonNameListener(v);
                break;
            case R.id.btn_gallery:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooser = Intent.createChooser(intent, "이미지를 불러옵니다");
                startActivityForResult(chooser, PICK_FROM_FILE);
                break;
            case R.id.btn_write:
                Intent intent1 = new Intent(getActivity().getApplicationContext(), WriteChemical.class);
                intent1.putExtra("type", "tab");
                startActivity(intent1);
                break;
        }
    }

    //자동완성을 위한 성분리스트 전체 항목을 불러온다.
    //write부분에서 사용
    private void getChemicalNameList() {
        if (WriteChemical.item == null) {

            retrofit = new Retrofit.Builder().baseUrl(ApiService.ADDRESS).build();
            apiService_chemical = retrofit.create(ApiServiceChemical.class);
            Call<ResponseBody> getList = apiService_chemical.getNameList("");
            getList.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        String tempList = response.body().string();
                        JSONObject jsonObject = new JSONObject(tempList);
                        WriteChemical.item = new String[jsonObject.length()];

                        for (int i = 0; i < jsonObject.length(); i++) {
                            WriteChemical.item[i] = jsonObject.getString(String.valueOf(i));
                        }

                        Log.e(">>>>>>>>>.TEST", tempList);
                        Log.e(">>>>>>>>>.TEST", Arrays.toString(WriteChemical.item));

                    } catch (IOException e) {
                        Log.i("retrofiError", e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(">>>>>>>>>.TEST", call.toString());
                }
            });
        }
    }//getChemicalNameList

    public void ButtonNameListener(View v) {
        DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doTakePhotoAction();
            }
        };
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(getActivity())
                .setTitle("제품명에 대한 촬영을 하겠습니다.")
                .setPositiveButton("확인", cameraListener)
                .setNegativeButton("취소", cancelListener)
                .show();
    }//ButtonNameListener

    public void ButtonDetailListener(View v) {
        DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doTakePhotoAction();
            }
        };
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(getActivity())
                .setTitle("화학성분에 대한 촬영을 하겠습니다.")
                .setPositiveButton("확인", cameraListener)
                .setNegativeButton("취소", cancelListener)
                .show();
    }//ButtonDetailListener

    /**
     * 카메라에서 이미지 가져오기
     */
    private void doTakePhotoAction() {
        //촬영 후 이미지 가져오기
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //전체 사진 파일에 대한 Uri
        mImageCaptureUri = getOutputMediaFileUri();
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        startActivityForResult(intent, PICK_FROM_CAMERA);
    }//doTakePhotoAction

    /**
     * Create a file Uri for saving an image
     */
    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }//getOutputMediaFileUri

    private File getOutputMediaFile() {
        //파일 경로 + 폴더
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        //해당 경로에 폴더가 없을 경우 새로 만들기
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(getString(R.string.app_name), "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //사진 파일
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName + ".jpg");
        return mediaFile;
    }//getOutputMediaFile

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {

            case PICK_FROM_FILE: {
                Uri selectImage = data.getData();
                uploadImage(selectImage);
                break;
            }
            case CROP_FROM_CAMERA: {
                // 크롭이 된 이후의 이미지를 넘겨 받습니다.
                final Bundle extras = data.getExtras();//전체 사진

                Bitmap cropPhoto = extras.getParcelable("data");//crop된 bitmap

                //bitmap -> jpg
                saveBitmaptoJpeg(cropPhoto, getString(R.string.app_name), fileName + "_crop");

                //crop photo file
                File cropPhotoFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + getString(R.string.app_name) + File.separator + fileName + "_crop.jpg");
                Log.e(">>>>>>>tempor", cropPhotoFile.getPath());

             /*   //photo file 서버로 보내기
                uploadImage(cropPhotoFile);*/

                //전체 사진 파일 + crop 사진 파일 지우기
/*                File f = new File(mImageCaptureUri.getPath());
                Log.e(">>>>>>>tempor", f.getPath());
                if (f.exists()) {
                    f.delete();
                }*/

                if (cropPhotoFile.exists()) {
                    cropPhotoFile.delete();
                }
                nextActivity();

                break;
            }

            case PICK_FROM_CAMERA: {
                // 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
                // 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.

                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");

                //crop한 이미지를 저장할 때
                intent.putExtra("outputX", 200);//crop한 이미지의 x축
                intent.putExtra("outputY", 200);//crop한 이미지의 y축
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_CAMERA);

                break;
            }//
        }//switch
    }//onActivityResult

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getActivity(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    @NonNull
    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(
                MediaType.parse(MULTIPART_FORM_DATA), descriptionString);
    }

    @NonNull
    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        File file = new File(getRealPathFromURI(fileUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), file);
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public void uploadImage(Uri uri) {
        UploadService service = MyRetrofit2.getRetrofit2().create(UploadService.class);

        File file = new File(getRealPathFromURI(uri));
        MultipartBody.Part body1 = prepareFilePart("image", uri);

        RequestBody description = createPartFromString("file");

        Call<ResponseBody> call = service.uploadFile(description, body1);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }//uploadImage

    public void uploadImage(File file) {
        UploadService service = MyRetrofit2.getRetrofit2().create(UploadService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), file);
        MultipartBody.Part prepareFilePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        RequestBody description = createPartFromString("file");

        Call<ResponseBody> call = service.uploadFile(description, prepareFilePart);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }//uploadImage

    /**
     * Image SDCard Save (input Bitmap -> saved file JPEG)
     * Writer intruder(Kwangseob Kim)
     *
     * @param bitmap : input bitmap file
     * @param folder : input folder name
     * @param name   : output file name
     */
    public static void saveBitmaptoJpeg(Bitmap bitmap, String folder, String name) {
        String ex_storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        // Get Absolute Path in External Sdcard
        String foler_name = "/" + folder + "/";
        String file_name = name + ".jpg";
        String string_path = ex_storage + foler_name;

        File file_path;
        try {
            file_path = new File(string_path);
            if (!file_path.isDirectory()) {
                file_path.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(string_path + file_name);

            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

        } catch (FileNotFoundException exception) {
            Log.e("FileNotFoundException", exception.getMessage());
        } catch (IOException exception) {
            Log.e("IOException", exception.getMessage());
        }
    }//saveBitmaptoJpeg

    public void nextActivity() {
        Intent intent = new Intent(getActivity().getApplicationContext(), Modification.class);

        inputData();

        intent.putExtra("result", arrayList);
        startActivity(intent);
    }

    public void inputData() {
        //임시 데이터
        TestDTO[] items = new TestDTO[5];

        String name = "에칠헥실메톡시신나메이트";

        for (int i = 0; i < items.length; i++) {
            items[i] = new TestDTO(i + 1, name, true, true, true);
            arrayList.add(items[i]);
        }

        arrayList.get(1).setBool(false, true, true);
        arrayList.get(2).setBool(true, false, true);
        arrayList.get(3).setBool(true, true, false);
        arrayList.get(4).setBool(false, true, false);
    }//inputData
}//FragmentSearch