
/*
CS-255 Getting started code for the assignment
I do not give you permission to post this code online
Do not post your solution online
Do not copy code
Do not use JavaFX functions or other libraries to do the main parts of the assignment:
	1. Creating a resized image (you must implement nearest neighbour and bilinear interpolation yourself
	2. Gamma correcting the image
	3. Creating the image which has all the thumbnails and event handling to change the larger image
All of those functions must be written by yourself
You may use libraries / IDE to achieve a better GUI
*/
import java.io.*;
import java.lang.Object;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;  
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;

public class test extends Application {
	short cthead[][][]; //store the 3D volume data set
	float grey[][][]; //store the 3D volume data set converted to 0-1 ready to copy to the image
	short min, max; //min/max value in the 3D volume data set
	ImageView TopView;
	int sliceOfLife;
	int newSize;
	boolean neighbour;

    @Override
    public void start(Stage stage) throws FileNotFoundException {
		stage.setTitle("CThead Viewer");
		
		try {
			ReadData();
		} catch (IOException e) {
			System.out.println("Error: The CThead file is not in the working directory");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			return;
		}
		
		//int width=1024, height=1024; //maximum size of the image
		//We need 3 things to see an image
		//1. We need to create the image
		Image top_image=GetSlice(76, 256, 256, 1); //go get the slice image
		//2. We create a view of that image
		TopView = new ImageView(top_image); //and then see 3. below

		//Create the simple GUI
		final ToggleGroup group = new ToggleGroup();

		RadioButton rb1 = new RadioButton("Nearest neighbour");
		rb1.setToggleGroup(group);
		rb1.setSelected(true);

		RadioButton rb2 = new RadioButton("Bilinear");
		rb2.setToggleGroup(group);

		Slider szslider = new Slider(32, 1024, 256);
		
		Slider gamma_slider = new Slider(.1, 4, 1);
		//Radio button changes between nearest neighbour and bilinear
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> ob, Toggle o, Toggle n) {
 
				if (rb1.isSelected()) {
					System.out.println("Radio button 1 clicked");
					SetResizedImage(newSize);
					neighbour = true;
					
				} else if (rb2.isSelected()) {
					System.out.println("Radio button 2 clicked");
					SetBiliniarImage(newSize);
					neighbour = false;
				}
            }
        });
		
		//Size of main image changes (slider)
		szslider.valueProperty().addListener(new ChangeListener<Number>() { 
			public void changed(ObservableValue <? extends Number >  
					observable, Number oldValue, Number newValue) { 
					newSize = newValue.intValue();
					if(neighbour) {
						SetResizedImage(newSize);
					}else {
						SetBiliniarImage(newSize);
					}
					
					
            } 
        });
		
		//Gamma value changes
		gamma_slider.valueProperty().addListener(new ChangeListener<Number>() { 
			public void changed(ObservableValue <? extends Number >  
						observable, Number oldValue, Number newValue) { 
				System.out.println(newValue.doubleValue());
			}
		});
		
		VBox root = new VBox();

		//Add all the GUI elements
        //3. (referring to the 3 things we need to display an image)
      	//we need to add it to the layout
		root.getChildren().addAll(rb1, rb2, gamma_slider,szslider, TopView);

		//Display to user
        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.show();
        
        ThumbWindow(scene.getX()+200, scene.getY()+200);
    }
    

	//Function to read in the cthead data set
	public void ReadData() throws IOException {
		//File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
		File file = new File("CThead");
		//Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find the equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
		int i, j, k; //loop through the 3D data set
		
		min=Short.MAX_VALUE; max=Short.MIN_VALUE; //set to extreme values
		short read; //value read in
		int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around
		
		cthead = new short[113][256][256]; //allocate the memory - note this is fixed for this data set
		grey= new float[113][256][256];
		//loop through the data reading it in
		for (k=0; k<113; k++) {
			for (j=0; j<256; j++) {
				for (i=0; i<256; i++) {
					//because the Endianess is wrong, it needs to be read byte at a time and swapped
					b1=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					b2=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					read=(short)((b2<<8) | b1); //and swizzle the bytes around
					if (read<min) min=read; //update the minimum
					if (read>max) max=read; //update the maximum
					cthead[k][j][i]=read; //put the short into memory (in C++ you can replace all this code with one fread)
				}
			}
		}
		System.out.println(min+" "+max); //diagnostic - for CThead this should be -1117, 2248
		//(i.e. there are 3366 levels of grey, and now we will normalise them to 0-1 for display purposes
		//I know the min and max already, so I could have put the normalisation in the above loop, but I put it separate here
		for (k=0; k<113; k++) {
			for (j=0; j<256; j++) {
				for (i=0; i<256; i++) {
					grey[k][j][i]=((float) cthead[k][j][i]-(float) min)/((float) max-(float) min);
				}
			}
		}
		
	}
	public void SetBiliniarImage(int newSize) {
		
		
		float val;
		double ratio = newSize/256.000;
		double percent = 256/newSize;

		
		
		WritableImage image = new WritableImage(newSize, newSize);
		PixelWriter image_writer = image.getPixelWriter();
		for(int y = 0; y < newSize; y++) {
			for(int x = 0; x < newSize; x++) {
				Color color = Color.color(1,1,1); //e.g. white pixel
				image_writer.setColor(x, y, color);
			}
		}
		System.out.println("------------------");
		//PixelReader image_reader = image.getPixelReader();
		if(ratio >= 1) {
			for (int y = 0 ; y < (256); y++) {
					for(int x = 0; x < (256); x++) {
							int j = y*newSize/256;  
							int i = x*newSize/256;
							
							val=grey[sliceOfLife][y][x]; //grey[sliceOfLife][j][i];
							Color color=Color.color(val,val,val);

							image_writer.setColor(i, j, color);
									
					}
			}
			TopView.setImage(null); 			
			TopView.setImage(image); 
			
			int x1,x2,y1,y2;
			x1 = 0;
			x2 = 0;
			y1 = 0;
			y2 = 0;
			
			Pair<Integer, Integer> v1 = new Pair<Integer, Integer>(x1, y1);
			Pair<Integer, Integer> v2 = new Pair<Integer, Integer>(x2, y1);
			Pair<Integer, Integer> v3 = new Pair<Integer, Integer>(x1, y2);
			Pair<Integer, Integer> v4 = new Pair<Integer, Integer>(x2, y2);
			Pair<Integer, Integer> v = new Pair<Integer, Integer>(0, 0);
			
			for(int y = 0 ; y < (newSize); y++) {
				boolean yIsPrimal = IsPrimalCoord(newSize, y);
				Pair<Integer, Integer> yHighAndLow = GetHigherAndLower(newSize, y);
				
				
				for(int x = 0; x < (newSize); x++) {


					v = new Pair<>(x,y);
					boolean xIsPrimal = IsPrimalCoord(newSize, x);
					if(xIsPrimal && yIsPrimal) {
						//System.out.println("Primal X:" + x + "Primal Y:" + y); //Checked
						v1 = v;
						v2 = v;
						v3 = v;
						v4 = v;
						//val= grey[sliceOfLife][GetPrimalCoord(newSize, x)][GetPrimalCoord(newSize, y)];

						//System.out.println("V1:" + v1.getKey() + " " + v1.getValue()); All Checked
						//System.out.println("V2:" + v2.getKey() + " " + v2.getValue());
						//System.out.println("V3:" + v3.getKey() + " " + v3.getValue());
						//System.out.println("V4:" + v4.getKey() + " " + v4.getValue());

						val = GetBiliniarColor(newSize, v, v1, v2, v3, v4);
						Color color=Color.color(val,val,val);
						image_writer.setColor(x, y, color);
					} 
					
					else if(xIsPrimal && !yIsPrimal) {
						
						y1 = yHighAndLow.getValue();
						y2 = yHighAndLow.getKey();
						//System.out.println("Y1:" + y1);
						//System.out.println("Y:" + y);
						//System.out.println("Y2:" + y2);
						
						
						v1 = new Pair<>(x, y1);
						v2 = new Pair<>(x, y1);
						v3 = new Pair<>(x, y2);
						v4 = new Pair<>(x, y2);
						
						val = GetBiliniarColor(newSize, v, v1, v2, v3, v4);
						Color color=Color.color(val,val,val);
						image_writer.setColor(x, y, color);
						
					} 
					
					else if(yIsPrimal && !xIsPrimal) {
						Pair<Integer, Integer> xHighAndLow = GetHigherAndLower(newSize, x);
						
						x1 = xHighAndLow.getValue();
						x2 = xHighAndLow.getKey();
						
						v1 = new Pair<>(x1, y);
						v2 = new Pair<>(x1, y);
						v3 = new Pair<>(x2, y);
						v4 = new Pair<>(x2, y);
						
						val = GetBiliniarColor(newSize, v, v1, v2, v3, v4);
						Color color=Color.color(val,val,val);
						image_writer.setColor(x, y, color);
					}
					
					else {
						
						y1 = yHighAndLow.getValue();
						y2 = yHighAndLow.getKey();
						
						Pair<Integer, Integer> xHighAndLow = GetHigherAndLower(newSize, x);
						
						x1 = xHighAndLow.getValue();
						x2 = xHighAndLow.getKey();
						
						v1 = new Pair<>(x1,y1);
						v2 = new Pair<>(x2,y1);
						v3 = new Pair<>(x1,y2);
						v4 = new Pair<>(x2,y2);
						
						val = GetBiliniarColor(newSize, v, v1, v2, v3, v4);
						Color color=Color.color(val,val,val);
						image_writer.setColor(x, y, color);
					}
					
					
				}
			}


		}
		else if(ratio < 1){
			for (int y = 0 ; y < (newSize); y++) {
				for(int x = 0; x < (newSize); x++) {
			
			        int j = y*256/newSize;
					int i = x*256/newSize;
					
					
					
					
					
					val=grey[sliceOfLife][j][i];
					
					Color color=Color.color(val,val,val);
					
					//Apply the new colour
					image_writer.setColor(x, y, color);
				}
			}
		}
		
		
		
		
		//PixelReader image_reader = image.getPixelReader();
		//Color a = image_reader.getColor(x, y);
		
		
		TopView.setImage(null); //clear the old image			
		TopView.setImage(image); //Update the GUI so the new image is displayed
	}
	
	public Pair<Integer, Integer> GetHigherAndLower(int newSize, int coord) {
		
		//The Problem
		
		float y = coord*256/newSize; //Primal Coordinate

		int higher,lower;// = (int)Math.ceil(y);
		// = (int)Math.floor(y);
		lower = coord;
		higher = lower;
		
		if(!IsPrimalCoord(newSize, coord)) {
			while(!IsPrimalCoord(newSize, higher)){
				higher++;
			}
			while(!IsPrimalCoord(newSize, lower)) {
				lower--;
			}
		}
		

		Pair<Integer, Integer> higherAndLower = new Pair<>(higher, lower);
		return higherAndLower;
	}
	
	public int GetPrimalCoord(int newSize, int coord) {
		
		int y = coord*256/newSize;
		
		if(y == 256) {
			y = 255;
		}
		
		
		return y;
	}
	
	public boolean IsPrimalCoord(int newSize, float coord) {


		float y = coord*256/newSize;
		float newCoord = (Math.round(y))*newSize/256;  

		
		return coord==newCoord;
	}
	
	public float GetColorX(int newSize, int x, Pair<Integer, Integer> v1, Pair<Integer, Integer> v2) {
		
		float val;
		
		/*
		System.out.println("Prime Y:" + GetPrimalCoord(newSize,v1.getValue()));
		System.out.println("Y:" + v1.getValue());
		System.out.println("Prime X:" + GetPrimalCoord(newSize, v1.getKey()));
		System.out.println("X:" + v1.getKey());
		System.out.println("NewSize:" + newSize);
		*/
		
		float val1 = grey[sliceOfLife][GetPrimalCoord(newSize,v1.getValue())][GetPrimalCoord(newSize, v1.getKey())];
		float val2 = grey[sliceOfLife][GetPrimalCoord(newSize,v2.getValue())][GetPrimalCoord(newSize, v2.getKey())];
		
		float notZero = 1;
		
		if((v2.getKey()-v1.getKey()) == 0) {
			val = val1;
		}
		else {
			notZero = (x-v1.getKey())/(v2.getKey()-v1.getKey()); //Problem
		}
		
		
		val = val1 + (val2 - val1)*(notZero);
		/*
			System.out.println("Val1:" + val1);
			System.out.println("Val2:" + val2);
			System.out.println("Val:" + val);
			System.out.println("Coord:" + x);
			System.out.println("V1X:" + v1.getKey());
			System.out.println("V2X:" + v2.getKey());
			System.out.println("X:" + notZero);
		*/
			
		return val;
	}
	
	public float GetColorY(int y, Pair<Integer, Integer> v1, Pair<Integer, Integer> v2, float val1, float val2) {
		
		float val;
		float notZero = 1;
		
		if((v2.getValue()-v1.getValue()) == 0) {
			
		}
		else {
			notZero = (y-v1.getValue())/(v2.getValue()-v1.getValue());
		}
		
		
		val = val1 + (val2 - val1)*notZero;
		/*
			System.out.println("Val1:" + val1);
			System.out.println("Val2:" + val2);
			System.out.println("Y:" + notZero);
		*/
		return val;
	}
	
	
	
	public float GetBiliniarColor(int newSize, Pair<Integer, Integer> v, Pair<Integer, Integer> v1, Pair<Integer, Integer> v2
			, Pair<Integer, Integer> v3, Pair<Integer, Integer> v4) {
		
		Pair<Integer, Integer> v5 = new Pair<Integer, Integer>(v.getKey(), v1.getValue());
		Pair<Integer, Integer> v6 = new Pair<Integer, Integer>(v.getKey(), v3.getValue());
		
		float val1 = GetColorX(newSize, v.getKey(), v1, v2);
		float val2 = GetColorX(newSize, v.getKey(), v3, v4);
		float val =  GetColorY(v.getValue(), v5, v6, val1, val2);

		
		/*
		System.out.println("Val1:" + val1);
		System.out.println("Val2:" + val2);
		System.out.println("FVal:" + val);
		*/
		return val;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void SetResizedImage(int newSize) {
		
		//Here's the basic code you need to update an image
        float val;
		
		WritableImage image = new WritableImage(newSize, newSize);

		PixelWriter image_writer = image.getPixelWriter();
		
		for (int y = 0 ; y < (newSize); y++) {
				for(int x = 0; x < (newSize); x++) {
					int j = y*256/newSize;
					int i = x*256/newSize;
					val=grey[sliceOfLife][j][i];
					
					Color color=Color.color(val,val,val);
					
					//Apply the new colour
					image_writer.setColor(x, y, color);
				}
		}
		
		TopView.setImage(null); //clear the old image			
		TopView.setImage(image); //Update the GUI so the new image is displayed
	}
	
	//Gets an image from slice 76
	public Image GetSlice(int slice, int xSize, int ySize, int imageSize) {
		WritableImage image = new WritableImage(xSize, ySize);
		//Find the width and height of the image to be process
		int width = (int)image.getWidth();
        int height = (int)image.getHeight();
        float val;

		//Get an interface to write to that image memory
		PixelWriter image_writer = image.getPixelWriter();

		//Iterate over all pixels
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
					//For each pixel, get the colour from the cthead slice 76
					val=grey[slice][y*imageSize][x*imageSize];
					Color color=Color.color(val,val,val);
					
					//Apply the new colour
					image_writer.setColor(x, y, color);
			}
		}
		return image;
	}
	
	public Image GetSlice(int slice) {
		return GetSlice(slice, 85, 85, 3);
	}
	
	public Image GetBigSlice() {
		WritableImage image = new WritableImage(1260, 1260);
        float val;

        PixelWriter image_writer = image.getPixelWriter();
		for(int n = 0; n < Math.ceil((float)grey.length/12) ; n++) {
			for(int i = 0; i < 12; i++) {
				if(i + n*12 < grey.length) {
			        for(int y = 0; y < 85; y++) {
						for(int x = 0; x < 85; x++) {
								//For each pixel, get the colour from the cthead slice 76
								val=grey[i + n*12][y*3][x*3];
								Color color=Color.color(val,val,val);
								
								//Apply the new colour
								image_writer.setColor(x + 85*(i) + (10*i), y + 85*(n) + (10*n), color);
						}
					}
				}
			}
			
		}
        
		return image;
	}

	
	public void ThumbWindow(double atX, double atY) {
		VBox ThumbLayout = new 	VBox();
		
		WritableImage thumb_image = new WritableImage(1260, 1260);
		ImageView thumb_view = new ImageView(GetBigSlice());
		ThumbLayout.getChildren().add(thumb_view);
		
		Scene ThumbScene = new Scene(ThumbLayout, thumb_image.getWidth(), thumb_image.getHeight());

		
		/*
		 * VBox root = new VBox();
		 * 
		 * 
		 * for(int n = 0; n < grey.length;) {
		 * 
		 * HBox box = new HBox();
		 * 
		 * for(int i = 0; i < 12; i++) {
		 * 
		 * if(grey.length > n) { box.getChildren().add(new ImageView(GetSlice(n)));
		 * System.out.println(n); n++;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * root.getChildren().add(box);
		 * 
		 * }
		 */
		
		// ThumbScene = new Scene(root, thumb_image.getWidth(), thumb_image.getHeight());
	
		
		
		
		
		//Add mouse over handler - the large image is change to the image the mouse is over
		ThumbScene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
			System.out.println(event.getX()+"  "+event.getY());
			
			int x = (int) (event.getX()/95);
			int y = (int)(event.getY()/95);
			sliceOfLife = (x + 12*y);
			
			boolean blank = false;
			
			if((((int) (event.getX())-10)/95) != x) {
				if(((int) (event.getY()-10)/95 != y )) {
					blank = true;;
					System.out.println(blank);
				}
			}
			
			if(((x + 12*y)  < grey.length && x < 12) && !blank) {
				TopView.setImage(GetSlice((sliceOfLife), 256, 256 , 1));
			}
			event.consume();
		});
	
		//Build and display the new window
		Stage newWindow = new Stage();
		newWindow.setTitle("CThead Slices");
		newWindow.setScene(ThumbScene);
		
		
		
		// Set position of second window, related to primary window.
		newWindow.setX(atX);
		newWindow.setY(atY);
		
		newWindow.show();
	}
	
    public static void main(String[] args) {
        launch();
    }

}