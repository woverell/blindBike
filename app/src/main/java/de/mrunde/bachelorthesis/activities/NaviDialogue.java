package de.mrunde.bachelorthesis.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;

import edu.csueb.ilab.blindbike.blindbike.R;

/**
 * Created by williamoverell on 5/5/15.
 */
public class NaviDialogue extends DialogFragment {

    public static NaviDialogue newInstance(int title) {
        NaviDialogue frag = new NaviDialogue();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.destination_flag)
                .setTitle(title)
                .setPositiveButton(R.string.navi_dialogue_okbutton_text,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                ((NaviActivity) getActivity())
                                        .doPositiveClick();
                            }
                        })
                .setNegativeButton(R.string.navi_dialogue_reroutebutton_text,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                ((NaviActivity) getActivity())
                                        .doNegativeClick();
                            }
                        }).create();
    }
}
