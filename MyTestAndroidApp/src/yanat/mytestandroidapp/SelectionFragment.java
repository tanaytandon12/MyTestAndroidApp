package yanat.mytestandroidapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class SelectionFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.selection, container, false);
		Button submitButton = (Button) view.findViewById(R.id.address_button);
		submitButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText addressText = (EditText) view.findViewById(R.id.address_text); 
				String address = addressText.getText().toString();
				Intent intent = new Intent(getActivity(),LocationActivity.class);
				intent.putExtra("address", address);
				startActivity(intent);
			}
		});
		return view;
	}
}
