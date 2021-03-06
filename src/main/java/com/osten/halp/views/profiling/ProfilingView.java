package com.osten.halp.views.profiling;

import com.osten.halp.api.model.gui.PopulatableView;
import com.osten.halp.api.model.profiling.AdaptiveFilter;
import com.osten.halp.api.model.profiling.ChangeDetector;
import com.osten.halp.api.model.profiling.Detection;
import com.osten.halp.api.model.profiling.PointsOfInterest;
import com.osten.halp.api.model.shared.DataModel;
import com.osten.halp.api.model.shared.DetectorModel;
import com.osten.halp.api.model.shared.FilterModel;
import com.osten.halp.api.model.shared.ProfileModel;
import com.osten.halp.api.model.statistics.Statistic;
import com.osten.halp.utils.FXMLUtils;
import com.osten.halp.views.main.MainWindowView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: server
 * Date: 2013-10-15
 * Time: 15:18
 * To change this template use File | Settings | File Templates.
 */
public class ProfilingView extends HBox implements Initializable, PopulatableView<Long>
{

	MainWindowView parentView;

	@FXML
	private ComboBox<ProfileModel.Profile> profileSelector;

	@FXML
	private ListView<String> statisticSelector;

	@FXML
	private ListView<Statistic.DataType> statisticTypeSelector;

	@FXML
	private VBox adaptiveFilterList;

	@FXML
	private VBox changeDetectorList;

	public ProfilingView( MainWindowView parentView )
	{
		this.parentView = parentView;
		FXMLUtils.load( this );
	}

	@FXML
	public void handleAnalyze( ActionEvent event )
	{
		getTabsSelectionModel().selectNext();

		System.out.println( "Applying filters" );
		Platform.runLater( new Runnable()
		{

			@Override
			public void run()
			{
				applyFiltersAndDetectors();
				parentView.rePopulateViews();
			}
		} );
	}

	private void applyFiltersAndDetectors()
	{
		for( Statistic<Long> statistic : getDataModel().getData() )
		{
			for( AdaptiveFilter<Long> filter : getFilterModel().getFiltersByStatisticName( statistic.getName() ) )
			{
				filter.reset();
				filter.adapt( statistic );

				for( ChangeDetector<Long> detector : getDetectorModel().getDetectorsByStatisticName( statistic.getName() ) )
				{
					detector.detect( filter );
					detector.printDetections();
				}
            filter.printAggregatedData();
			}
		}
		generatePointsOfInterests();
	}

	private void generatePointsOfInterests(){
		HashMap<Statistic<Long>, List<Detection<Long>>> statisticDetectionMap = new HashMap<Statistic<Long>, List<Detection<Long>>>();
		for( Statistic<Long> statistic : getDataModel().getData()){
			statisticDetectionMap.put( statistic, new ArrayList<Detection<Long>>() );
			for( ChangeDetector<Long> changeDetector : getDetectorModel().getDetectorsByStatisticName( statistic.getName() ) ) {
				statisticDetectionMap.put( statistic, changeDetector.getDetections() );
			}
		}
		getProfileModel().generatePointsOfInterests( statisticDetectionMap );
	}

	public SelectionModel getTabsSelectionModel()
	{
		return parentView.getSelectionModel();
	}

	@Override
	public void populate( DataModel<Long> dataModel, FilterModel<Long> filterModel, DetectorModel<Long> detectorModel, ProfileModel<Long> profileModel )
	{
		System.out.println( "ProfilingView Repopulated using: " );
		dataModel.printModel(); //TODO Remove printout

		statisticSelector.getItems().clear();
		statisticTypeSelector.getItems().clear();
		adaptiveFilterList.getChildren().clear();
		changeDetectorList.getChildren().clear();

		statisticSelector.getItems().addAll( FXCollections.observableList( getDataModel().getStatisticNames() ) );
		statisticSelector.getSelectionModel().selectFirst();

		Statistic<Long> selectedStatistic = getDataModel().getDataByName( statisticSelector.getSelectionModel().getSelectedItem() );

		statisticTypeSelector.getItems().addAll( Statistic.DataType.values() );
		statisticTypeSelector.getSelectionModel().select( selectedStatistic.getType() );
	}

	private void setFiltersAndDetectorsByProfileAndStatisticTypes()
	{
		ProfileModel.Profile selectedProfile = getSelectedProfile();
		getProfileModel().selectProfile( selectedProfile );

		if( selectedProfile != ProfileModel.Profile.Custom )
		{
			getFilterModel().resetModel();
			getDetectorModel().resetModel();
		}

		//TODO Handle the ALL profile here when time

		for( Statistic<Long> statistic : getDataModel().getData() )
		{
			AdaptiveFilter.FilterType filterType = getProfileModel().getFilterByDataType( statistic.getType() );
			ChangeDetector.DetectorType detectorType = getProfileModel().getDetectorByDataType( statistic.getType() );
			if(filterType != null && detectorType != null ){
				getFilterModel().createFilter( statistic.getName(), filterType );
				getDetectorModel().createDetector( statistic.getName(), detectorType );
			}
		}


	}

	@Override
	public DataModel<Long> getDataModel()
	{
		return parentView.getDataModel();
	}

	@Override
	public FilterModel<Long> getFilterModel()
	{
		return parentView.getFilterModel();
	}

	public DetectorModel<Long> getDetectorModel()
	{
		return parentView.getDetectorModel();
	}

	@Override
	public ProfileModel<Long> getProfileModel()
	{
		return parentView.getProfileModel();
	}

	@Override
	public void initialize( URL url, ResourceBundle resourceBundle )
	{

		profileSelector.getItems().addAll( ProfileModel.Profile.values() );
		profileSelector.getSelectionModel().selectFirst();

		profileSelector.getSelectionModel().selectedItemProperty().addListener( handleProfileSelected );
		statisticSelector.getSelectionModel().selectedItemProperty().addListener( handleStatisticSelected );
		statisticTypeSelector.getSelectionModel().selectedItemProperty().addListener( handleStatisticTypeSelected );
	}

	public String getSelectedStatistic()
	{
		return statisticSelector.getSelectionModel().getSelectedItem();
	}

	public Statistic.DataType getSelectedStatisticType()
	{
		return statisticTypeSelector.getSelectionModel().getSelectedItem();
	}

	public ProfileModel.Profile getSelectedProfile()
	{
		return profileSelector.getSelectionModel().getSelectedItem();
	}

	private ChangeListener handleStatisticSelected = new ChangeListener()
	{

		@Override
		public void changed( ObservableValue observableValue, Object o, Object o2 )
		{
			if( !statisticSelector.getItems().isEmpty() )
			{
				String selectedItem = getSelectedStatistic();

				Statistic.DataType type = getDataModel().getDataByName( selectedItem ).getType();
				statisticTypeSelector.getSelectionModel().select( type );

				System.out.println( getSelectedStatistic().toString() + " of type " + type + " selected." );
			}
		}
	};

	private ChangeListener<Statistic.DataType> handleStatisticTypeSelected = new ChangeListener<Statistic.DataType>()
	{
		@Override
		public void changed( ObservableValue<? extends Statistic.DataType> observableValue, Statistic.DataType oldType, Statistic.DataType newType )
		{
			if( !statisticTypeSelector.getItems().isEmpty() )
			{
				String name = getSelectedStatistic();

				System.out.println( name + " is of statistic-type " + newType );
				getDataModel().getDataByName( name ).setType( newType );

				if( getSelectedProfile() != ProfileModel.Profile.Custom){
					setFiltersAndDetectorsByProfileAndStatisticTypes();
				}

				//Filters
				SelectableFilterView filterView = new SelectableFilterView( ProfilingView.this );
				adaptiveFilterList.getChildren().clear();
				adaptiveFilterList.getChildren().add( filterView );

				//Detectors
				SelectableDetectorView detectorView = new SelectableDetectorView( ProfilingView.this );
				changeDetectorList.getChildren().clear();
				changeDetectorList.getChildren().add( detectorView );
			}
		}
	};


	private ChangeListener handleProfileSelected = new ChangeListener()
	{

		@Override
		public void changed( ObservableValue observableValue, Object o, Object o2 )
		{
			setFiltersAndDetectorsByProfileAndStatisticTypes();
			System.out.println( getSelectedProfile().toString() + "-profile selected" );
		}
	};
}
