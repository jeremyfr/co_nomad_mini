package com.eads.co.nomad;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.eads.co.nomad.PanAndZoomListener.Anchor;
import com.eads.co.nomad.R.color;

/**
 * Class used to manage the display of the documentation.
 * 
 * @author Nicolas Bruniquel
 * @author Jérémy Fricou
 * @author Florian Lefebvre
 * @author Benjamin Louradour
 * @author Guillaume Saas
 */

public class AMMAnnexes extends Activity implements PropertyChangeListener,
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private DataParsing parser;

	private static int xmax; // largeur maximale de la zone de texte ou de
								// l'annexe.
	private static int ymax; // ordonnée d'apparition de la flèche basse.

	private static int xseparator = 0; // largeur de la barre de séparation.

	private static int t2 = 200; // temps avant de récupérer largeur et hauteur.

	private ScrollView scrollView;
	
	private int scrollX;
	private int scrollY;

	private LinearLayout layout; // layout global contenant documentation et
									// annexes.

	private WebView warningWV, jobSetUpWV, procedureWV, closeUpWV, toolsWV,
			picturesWV;
	private TextView dateRevisionTV;
	private LinearLayout warnings, jobSetUp, procedure, closeUp, tools,
			pictures;

	private WebView clickedWB; // WebView contenant le lien de l'annexe cliquée.
	private String annexe; // Nom de l'annexe.

	private LinearLayout annexLayout; // layout de l'annexe.
	private TextView titreAnnexe; // titre de l'annexe
	private ImageButton closeAnnexButton; // bouton femer.
	private ImageButton fullScreenAnnexButton; // bouton plein ecran.

	// Pour les annexes multiples
	private DrawerLayout mDrawerLayout;
	private ListView listview;

	// Création de la ArrayList qui nous permettra de remplir la listView
	ArrayList<HashMap<String, Object>> listItem;
	HashMap<String, Object> map;

	// Pour le multitouch
	private ImageView annexImg;
	private FrameLayout layoutImg;

	private String title;

	public AnnexesState state = AnnexesState.NOT_DISPLAYED; // état de l'annexe.

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getWidthHeight();
	}

	@Override
	public void onResume() {
		super.onResume();
		getWidthHeight();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.amm_annexes);

		layout = (LinearLayout) findViewById(R.id.layout_amm);

		scrollView = (ScrollView) findViewById(R.id.scrollView);

		annexLayout = (LinearLayout) findViewById(R.id.annexLayout);
		titreAnnexe = (TextView) findViewById(R.id.annexTitle);
		closeAnnexButton = (ImageButton) findViewById(R.id.closeAnnexButton);
		fullScreenAnnexButton = (ImageButton) findViewById(R.id.fullScreenAnnexButton);

		// Pour le multitouch
		annexImg = (ImageView) findViewById(R.id.annexImage);
		layoutImg = (FrameLayout) findViewById(R.id.layoutImage);
		PanAndZoomListener pan = new PanAndZoomListener(layoutImg, annexImg,
				Anchor.TOPLEFT);
		annexImg.setOnTouchListener(pan);
		pan.addPropertyChangeListener(this);

		// Pour les annexes multiples
		listview = (ListView) findViewById(R.id.listview);
		listview.setSelector(R.drawable.selector);

		listItem = new ArrayList<HashMap<String, Object>>();
		// Remplissage des fonctions sur le navigation drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerLockMode(1, Gravity.END);

		// Récupération de la largeur et de la hauteur du layout
		getWidthHeight();

		map = new HashMap<String, Object>();
		map.put("titre", "Close All");
		map.put("img", String.valueOf(R.drawable.close));
		map.put("webview", null);
		listItem.add(map);

		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> a, View v, int position,
					long id) {
				if (position == 0) {
					supprimeTout();
				} else {
					HashMap<String, Object> map = (HashMap<String, Object>) listview
							.getItemAtPosition(position);
					annexe = (String) map.get("titre");
					titreAnnexe.setText(annexe);
					int resID = getResources()
							.getIdentifier((String) map.get("img"), "drawable",
									"package.name");
					annexImg.setImageResource(resID);
					clickedWB = (WebView) map.get("webview");
					scrollX = scrollView.getScrollX();
					scrollY = scrollView.getScrollY();
					setAnnexeX(xseparator / 3);
					state = AnnexesState.DISPLAYED_FULLSCREEN;
				}

			}
		});


		// Listener sur le bouton fermer.
		closeAnnexButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (state) {
				case NOT_DISPLAYED:
					break;
				case DISPLAYED_FULLSCREEN:
					supprimeElt(titreAnnexe.getText().toString(), clickedWB);
					break;
				}
			}
		});

		// Listener sur le bouton plein ecran
		fullScreenAnnexButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (state) {
				case NOT_DISPLAYED:
					break;
				case DISPLAYED_FULLSCREEN:
					setAnnexeX(xmax + xseparator / 3);
					scrollView.setScrollX(scrollX);
					scrollView.setScrollY(scrollY);
					state = AnnexesState.NOT_DISPLAYED;
					break;
				}
			}
		});

		/* Retrieving the documentation to show */
		Bundle bundle = getIntent().getExtras();
		String ammPart = "";
		if (bundle != null) {
			ammPart = (String) bundle.get("task");
			title = bundle.getString("titre");
		}

		InputStream input = null;
		try {
			input = getApplicationContext().getAssets().open(ammPart + ".xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			parser = new DataParsing(input);
			this.setTitle(parser.getTitle() + " " + title);
			SwitchTaskManager taskManager = new SwitchTaskManager(this, this);

			HashMap<String, String> h = ((History) this.getApplication())
					.getHistory();
			h.put(ammPart, parser.getTitle());

			/* Date */
			dateRevisionTV = (TextView) findViewById(R.id.date_text);
			displayLastRevision(ammPart);
			// Add procedure to used procedures
			File file = new File(getApplicationContext().getFilesDir(),"proceduresUsed.txt");
			if(!procedureUsed(file, ammPart)){
				FileWriter logWriter = new FileWriter(file);                  
		        BufferedWriter out = new BufferedWriter(logWriter); 
		        out.write(ammPart);
		        out.newLine();
		        out.close();
			}
			/* Warnings part */
			warnings = (LinearLayout) findViewById(R.id.warnings);
			warnings.setOnClickListener(manageWarnings);
			((ImageView) findViewById(R.id.stateWarning))
					.setTag(R.drawable.expand);
			warningWV = ((WebView) findViewById(R.id.warnings_text));
			warningWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getWarnings(), "text/html", "UTF-8", null);
			warningWV.setWebViewClient(taskManager);
			warningWV.getSettings().setJavaScriptEnabled(true);

			/* Job Setup part */
			jobSetUp = (LinearLayout) findViewById(R.id.jobSetUp);
			jobSetUp.setOnClickListener(manageJobSetUp);
			collapse((LinearLayout) findViewById(R.id.jobSetUp_layout));
			((ImageView) findViewById(R.id.stateJobSetUp))
					.setTag(R.drawable.collapse);
			jobSetUpWV = ((WebView) findViewById(R.id.jobSetUp_text));
			jobSetUpWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getJobSetUp(), "text/html", "UTF-8", null);
			jobSetUpWV.setWebViewClient(taskManager);
			jobSetUpWV.getSettings().setJavaScriptEnabled(true);
			/* Procedure part */
			procedure = (LinearLayout) findViewById(R.id.procedure);
			procedure.setOnClickListener(manageProcedure);
			collapse((LinearLayout) findViewById(R.id.procedure_layout));
			((ImageView) findViewById(R.id.stateProcedure))
					.setTag(R.drawable.collapse);
			procedureWV = ((WebView) findViewById(R.id.procedure_text));
			procedureWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getProcedure(), "text/html", "UTF-8", null);
			procedureWV.setWebViewClient(taskManager);
			procedureWV.getSettings().setJavaScriptEnabled(true);
			/* Close Up part */
			closeUp = (LinearLayout) findViewById(R.id.closeUp);
			closeUp.setOnClickListener(manageCloseUp);
			collapse((LinearLayout) findViewById(R.id.closeUp_layout));
			((ImageView) findViewById(R.id.stateCloseUp))
					.setTag(R.drawable.collapse);
			closeUpWV = ((WebView) findViewById(R.id.closeUp_text));
			closeUpWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getCloseUp(), "text/html", "UTF-8", null);
			closeUpWV.setWebViewClient(taskManager);
			closeUpWV.getSettings().setJavaScriptEnabled(true);
			/* Tools part */
			tools = (LinearLayout) findViewById(R.id.tools);
			tools.setOnClickListener(manageTools);
			collapse((LinearLayout) findViewById(R.id.tools_layout));
			((ImageView) findViewById(R.id.stateTools))
					.setTag(R.drawable.collapse);
			toolsWV = ((WebView) findViewById(R.id.tools_text));
			toolsWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getTools(), "text/html", "UTF-8", null);
			toolsWV.setWebViewClient(taskManager);
			toolsWV.getSettings().setJavaScriptEnabled(true);
			/* Pictures part */
			pictures = (LinearLayout) findViewById(R.id.pictures);
			pictures.setOnClickListener(managePictures);
			collapse((LinearLayout) findViewById(R.id.pictures_layout));
			((ImageView) findViewById(R.id.statePictures))
					.setTag(R.drawable.collapse);
			picturesWV = ((WebView) findViewById(R.id.pictures_text));

			picturesWV.setInitialScale(100);

			picturesWV.loadDataWithBaseURL("file:///android_asset/",
					parser.getPictures(), "text/html", "UTF-8", null);
			picturesWV.setWebViewClient(taskManager);
			picturesWV.getSettings().setJavaScriptEnabled(true);
			picturesWV.getLayoutParams().height = pictures.getLayoutParams().height;
		} catch (Exception e) {
			ammPart = ammPart.substring(ammPart.lastIndexOf('/') + 1);
			this.setTitle("Procedure " + ammPart + " introuvable");
			HashMap<String, String> h = ((History) this.getApplication())
					.getHistory();
			h.put(ammPart, ammPart + " - Unknown task");
			e.printStackTrace();
		}

		/* Gestion du slider gauche (historique) */
		ListView historique = (ListView) findViewById(R.id.left_drawer);
		historique.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		historique.setAdapter(new ArrayAdapter<String>(this,
				R.layout.historic_row, ((History) this.getApplication())
						.getTitles()));
		historique.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				onHistoricItemClick(position);
			}
		});
	}

	private void displayLastRevision(String procedure) {
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		File file = new File(getApplicationContext().getFilesDir(),"proceduresUsed.txt");
		if(procedureUsed(file, procedure)){
			dateRevisionTV.setText("Last revision : "
					+ formatter.format(parser.getLastRevision()));
		}else{
			dateRevisionTV.setPadding(70, 0, 0, 0);
			dateRevisionTV.setTextColor(getResources().getColor(color.red));
			dateRevisionTV.setTypeface(null, Typeface.BOLD);
			dateRevisionTV.setGravity(Gravity.LEFT);
			dateRevisionTV.setText("New revision found : "
					+ formatter.format(parser.getLastRevision()));
		}
	}

	@SuppressWarnings("resource")
	private boolean procedureUsed(File file, String procedure){
		InputStream instream;
		boolean procedureFound = false;
		try {
			instream = new FileInputStream(file);
			if (instream != null) {
				BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
				String line = buffreader.readLine();
				while (line != null && procedureFound == false) {
					if(line.contains(procedure)){
						procedureFound = true;
					}
					line = buffreader.readLine();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return procedureFound;
	}

	// Affiche l'annexe.
	private void setAnnexeX(int x) {
		mDrawerLayout.setDrawerLockMode(0, Gravity.END);
		scrollView.setLayoutParams(new LayoutParams(x - xseparator / 3,
				LayoutParams.MATCH_PARENT));
		annexLayout.setLayoutParams(new LayoutParams(xmax - x - xseparator / 3,
				ymax));
		// setInfobulle();
	}

	// Affiche le titre de l'annexe et met l'image de l'annexe
	private void setTitleAndImgAnnexe(CharSequence text, String img, WebView wb) {
		int resID = getResources().getIdentifier(img, "drawable",
				"package.name");
		annexImg.setImageResource(resID);
		titreAnnexe.setText(text);
		int position = trouveDansListe(titreAnnexe.getText().toString(), wb) - 1;
		listview.performItemClick(
				listview.getAdapter().getView(position, null, null), position,
				position);
	}

	public void onAnnexeClic(WebView webView, String annexe) {
		switch (state) {
		case NOT_DISPLAYED:
			this.annexe = annexe;
			clickedWB = webView;
			scrollX = scrollView.getScrollX();
			scrollY = scrollView.getScrollY();
			setAnnexeX(xseparator / 3);
			// Image a changer
			ajouteList(annexe, String.valueOf(R.drawable.ata), clickedWB);
			// Image a changer
			setTitleAndImgAnnexe(annexe, String.valueOf(R.drawable.ata),
					clickedWB);
			state = AnnexesState.DISPLAYED_FULLSCREEN;
			break;
		case DISPLAYED_FULLSCREEN:
			break;
		}
	}

	private void getWidthHeight() {
		// Récupération de la largeur et de la hauteur du layout.
		Timer t = new Timer();
		class SetMax extends TimerTask {
			@Override
			public void run() {
				ymax = layout.getHeight();
				xmax = layout.getWidth();
			}
		}
		t.schedule(new SetMax(), t2);
	}

	// Fonction pour supprimer tout les éléments de la liste
	private void supprimeTout() {
		listItem = new ArrayList<HashMap<String, Object>>();
		listview.invalidateViews();
		map = new HashMap<String, Object>();
		map.put("titre", "Close All");
		map.put("img", String.valueOf(R.drawable.close));
		map.put("webview", null);
		listItem.add(map);
		setAnnexeX(xmax + xseparator / 3);
		mDrawerLayout.setDrawerLockMode(1, Gravity.END);
		state = AnnexesState.NOT_DISPLAYED;
	}

	// Fonction qui supprime un élément de la listview et agit en conséquence
	// suivant la prèsence d'autres annexes
	private void supprimeElt(String titre, WebView wb) {
		if (listItem.size() != 2) {
			int indice = trouveDansListe(titre, wb);
			listItem.remove(indice - 1);
			listview.invalidateViews();
			int position = 1;
			listview.performItemClick(
					listview.getAdapter().getView(position, null, null),
					position, position);
		} else {
			listItem.remove(trouveDansListe(titre, wb) - 1);
			listview.invalidateViews();
			setAnnexeX(xmax + xseparator / 3);
			mDrawerLayout.setDrawerLockMode(1, Gravity.END);
			state = AnnexesState.NOT_DISPLAYED;
		}
	}

	// Fonction pour trouver l'indice de l'élément titre.
	private int trouveDansListe(String titre, WebView wb) {
		Iterator<HashMap<String, Object>> iterateur = listItem.iterator();
		int numero = 0;
		boolean test = false;
		while (!test && iterateur.hasNext()) {
			HashMap<String, Object> mapElt = iterateur.next();
			test = (mapElt.get("titre").equals(titre) && mapElt.get("webview")
					.equals(wb));
			numero++;
		}
		return numero;
	}

	// Teste si l'objet de titre titre est dans la listview
	private boolean testeObjetDansListe(String titre, WebView wb) {
		Iterator<HashMap<String, Object>> iterateur = listItem.iterator();
		boolean test = false;
		while (!test && iterateur.hasNext()) {
			HashMap<String, Object> mapElt = iterateur.next();
			test = (mapElt.get("titre").equals(titre) && mapElt.get("webview")
					.equals(wb));
		}
		return test;
	}

	// Ajoute l'élément titre avec sont image a la listview
	private void ajouteList(String titre, String img, WebView wb) {
		if (!testeObjetDansListe(titre, wb)) {
			map = new HashMap<String, Object>();
			map.put("titre", titre);
			map.put("img", img);
			map.put("webview", wb);
			listItem.add(map);
			// Création d'un SimpleAdapter qui se chargera de mettre les items
			// présent dans notre list (listItem) dans la vue affichage_annexes
			SimpleAdapter mSchedule = new SimpleAdapter(this.getBaseContext(),
					listItem, R.layout.affiche_annexes, new String[] { "img",
							"titre" }, new int[] { R.id.listImage,
							R.id.listTitreAnnexe });
			listview.setAdapter(mSchedule);
		}
	}

	private void onHistoricItemClick(int position) {
		Intent i = new Intent(this, AMMAnnexes.class);
		i.putExtra("task", ((History) this.getApplication()).getKeyAt(position));
		this.startActivity(i);
	}

	private View.OnClickListener manageWarnings = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.warnings_layout, R.id.stateWarning);
		}
	};
	private View.OnClickListener manageJobSetUp = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.jobSetUp_layout, R.id.stateJobSetUp);
		}
	};
	private View.OnClickListener manageProcedure = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.procedure_layout, R.id.stateProcedure);
		}
	};
	private View.OnClickListener manageCloseUp = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.closeUp_layout, R.id.stateCloseUp);
		}
	};
	private View.OnClickListener manageTools = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.tools_layout, R.id.stateTools);
		}
	};
	private View.OnClickListener managePictures = new View.OnClickListener() {
		public void onClick(View v) {
			expandOrCollapse(R.id.pictures_layout, R.id.statePictures);
		}
	};

	/**
	 * Expand or collapse a documentation part.
	 * 
	 * @param part
	 *            , the part to expand or collapse
	 * @param icon
	 *            , the icon to manage
	 */
	private void expandOrCollapse(int part, int icon) {
		int tag = Integer.parseInt(((ImageView) findViewById(icon)).getTag()
				.toString());
		if (tag == R.drawable.collapse) {
			expand(part, icon);
		} else {
			collapse(part, icon);
		}
	}

	/**
	 * Expand a documentation part.
	 * 
	 * @param part
	 *            , the part to expand
	 * @param icon
	 *            , the icon to manage
	 */
	private void expand(int part, int icon) {
		int tag = Integer.parseInt(((ImageView) findViewById(icon)).getTag()
				.toString());
		if (tag != R.drawable.expand) {
			expand((LinearLayout) findViewById(part));
			((ImageView) findViewById(icon))
					.setImageResource(R.drawable.expand);
			((ImageView) findViewById(icon)).setTag(R.drawable.expand);
		}
	}

	/**
	 * Collapse a documentation part.
	 * 
	 * @param part
	 *            , the part to collapse
	 * @param icon
	 *            , the icon to manage
	 */
	private void collapse(int part, int icon) {
		int tag = Integer.parseInt(((ImageView) findViewById(icon)).getTag()
				.toString());
		if (tag != R.drawable.collapse) {
			collapse((LinearLayout) findViewById(part));
			((ImageView) findViewById(icon))
					.setImageResource(R.drawable.collapse);
			((ImageView) findViewById(icon)).setTag(R.drawable.collapse);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_amm, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MenuApp.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_expandAll:
			expand(R.id.warnings_layout, R.id.stateWarning);
			expand(R.id.jobSetUp_layout, R.id.stateJobSetUp);
			expand(R.id.procedure_layout, R.id.stateProcedure);
			expand(R.id.closeUp_layout, R.id.stateCloseUp);
			expand(R.id.tools_layout, R.id.stateTools);
			expand(R.id.pictures_layout, R.id.statePictures);
			return true;
		case R.id.menu_collapseAll:
			collapse(R.id.warnings_layout, R.id.stateWarning);
			collapse(R.id.jobSetUp_layout, R.id.stateJobSetUp);
			collapse(R.id.procedure_layout, R.id.stateProcedure);
			collapse(R.id.closeUp_layout, R.id.stateCloseUp);
			collapse(R.id.tools_layout, R.id.stateTools);
			collapse(R.id.pictures_layout, R.id.statePictures);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void expand(final View v) {
		v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		final int targtetHeight = v.getMeasuredHeight();

		v.getLayoutParams().height = 0;
		v.setVisibility(View.VISIBLE);
		Animation a = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				v.getLayoutParams().height = interpolatedTime == 1 ? LayoutParams.WRAP_CONTENT
						: (int) (targtetHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int) (targtetHeight / v.getContext().getResources()
				.getDisplayMetrics().density));
		v.startAnimation(a);
	}

	private void collapse(final View v) {
		final int initialHeight = v.getMeasuredHeight();

		Animation a = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				if (interpolatedTime == 1) {
					v.setVisibility(View.GONE);
				} else {
					v.getLayoutParams().height = initialHeight
							- (int) (initialHeight * interpolatedTime);
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int) (initialHeight / v.getContext().getResources()
				.getDisplayMetrics().density));
		v.startAnimation(a);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getNewValue().equals("FULLSCREEN")) {
			fullScreenAnnexButton.performClick();
		}
	}

}
