/*
получение расписания URFU (текст возвращается в UTF-8 кодировке)
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;

class urfutable {
	public static void main( String[] args ) {
		try {
			//получаем код страницы
			URL address = new URL( "https://urfu.ru/api/schedule/groups/lessons/984802/" + getDate() + "/" );
			HttpURLConnection connection = ( HttpURLConnection )address.openConnection();
			connection.setRequestProperty( "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11" );
			ArrayList<String> sheet = getSheet( connection );
			
			ArrayList<String> data = getData( sheet );
			String[][] table = getTable( data );
			outTable( table );
			
			connection.disconnect();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	//вывод массива String[][]
	public static void outTable( String[][] array ) {
		int[] colsize = new int[array[0].length];
		for ( int i = 0; i < array.length; i++ ) {
			for ( int i2 = 0; i2 < array[0].length; i2++ ) {
				if ( array[i][i2] != null ) {
					if ( colsize[i2] < array[i][i2].length()) {
						colsize[i2] = array[i][i2].length();
					}
				}
			}
		}
		
		for ( int i = 0; i < array.length; i++ ) {
			for ( int i2 = 0; i2 < array[0].length; i2++ ) {
				if ( array[i][i2] != null ) {
					System.out.print( array[i][i2] );
					for ( int i3 = array[i][i2].length(); i3 < colsize[i2]; i3++ ) {
						System.out.print( " " );
					}
				} else {
					for ( int i3 = 0; i3 < colsize[i2]; i3++ ) {
						System.out.print( " " );
					}
				}
				System.out.print( " | " );
			}
			System.out.println();
		}
	}
	
	//получаем текст в кодировке windows-1251
	public static String getStringUTF( String text ) {
		try {
			String newtext = new String( text.getBytes( "windows-1251" ), "utf-8" );
			return newtext;
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	//делаем отступы после номеров строки
	public static String getOffset( int number, int maxnumber ) {
		String num = Integer.toString( number );
		String numlines = Integer.toString( maxnumber );
		int numspace = numlines.length() - num.length();
		String offset = "";
		for ( int i = 0; i < numspace; i++ ) {
			offset += " ";
		}
		return offset;
	}
	
	//преобразуем ArrayList в String[][] массив
	public static String[][] getTable( ArrayList<String> list ) {
		String[][] table = new String[list.size()][getColNum( list )];
		for ( int i = 0; i < list.size(); i++ ) {
			String line = list.get( i );
			String text = "";
			int col = 0;
			for ( int i2 = 0; i2 < line.length(); i2++ ) {
				if (( line.charAt( i2 ) != '|' ) & ( i2 != line.length() - 1 )) {
					text += line.charAt( i2 );
				} else {
					table[i][col] = text;
					col += 1;
					text = "";
					continue;
				}
			}
		}
		return table;
	}
	
	//получение количества столбцов массива
	public static int getColNum( ArrayList<String> list ) {
		int max = 0;
		for ( int i = 0; i < list.size(); i++ ) {
			String line = list.get( i );
			int lineMax = 0;
			for ( int i2 = 0; i2 < line.length(); i2++ ) {
				if ( line.charAt( i2 ) == '|' ) {
					lineMax += 1;
				}
			}
			if ( lineMax > max ) {
				max = lineMax;
			}
		}
		return max + 1;
	}
	
	//получаем новый ArrayList с данными
	public static ArrayList<String> getData( ArrayList<String> sheet ) {
		ArrayList<String> newtable = new ArrayList<String>();
		
		//получаем строки таблицы
		ArrayList<String> tableLines = getValue( sheet, "<tr class=", "</tr>" );
		
		//удаляем лишние пробелы и пустые строки
		for ( int i = 0; i < tableLines.size(); i++ ) {
			String ln = tableLines.get( i );
			tableLines.set( i, delSpace( ln ));
		}
		tableLines = delEmpty( tableLines );
		
		String saveday = "";
		for ( int i = 0; i < tableLines.size(); i++ ) {
			ArrayList<String> day = getValueString( tableLines.get( i ), "<b>", "</b>" );
			if ( day != null ) {
				saveday = delEmpty( day ).get( 0 );
			}
			
			ArrayList<String> time = getValueString( tableLines.get( i ), "<td class=\"shedule-weekday-time\">", "</td>" );
			ArrayList<String> subjects = getValueString( tableLines.get( i ), "<dd>", "</dd>" );
			ArrayList<String> dt = getValueString( tableLines.get( i ), "<dt>", "</dt>" );
			
			if (( subjects != null ) & ( time != null ) & ( dt != null )) {
				time = delEmpty( time );
				subjects = delEmpty( subjects );
				dt = delEmpty( dt );
				ArrayList<String> cabinet = getValue( dt, "\"_blank\">", "</a>" );
				ArrayList<String> teacher = getValue( dt, "<span class=\"teacher\">", "</span>" );
				
				String line = saveday + "|" + time.get( 0 ) + "|" + subjects.get( 0 );
				
				if (( cabinet != null ) & ( teacher != null )) {
					cabinet = delEmpty( cabinet );
					teacher = delEmpty( teacher );
					line += "|" + cabinet.get( 0 );
					for ( int i2 = 0; i2 < teacher.size(); i2++ ) {
						line += "|" + teacher.get( i2 );
					}
				}
				
				newtable.add( line );
			}
		}
		return newtable;
	}
	
	//убираем пробелы
	public static String delSpace( String line ) {
		String text = "";
		for ( int i = 0; i < line.length(); i++ ) {
			if ( i + 1 < line.length()) {
				if (( line.charAt( i ) == ' ' ) & ( line.charAt( i + 1 ) != ' ' )) {
					text += ' ';
				}
			}
			if ( line.charAt( i ) != ' ' ) {
				text += line.charAt( i );
			}
		}
		return text;
	}
	
	//удаление пустых строк в ArrayList
	public static ArrayList<String> delEmpty( ArrayList<String> sheet ) {
		for ( int i = 0; i < sheet.size(); i++ ) {
			String line = sheet.get( i );
			if ( line.equals( "" ) == true ) {
				sheet.remove( i );
			}
		}
		return sheet;
	}
	
	//получаем внутренности тега(ов) в строке
	public static ArrayList<String> getValueString( String line, String startTeg, String finalTeg ) {
		ArrayList<String> lineList = new ArrayList<String>();
		lineList.add( line );
		ArrayList<String> values = getValue( lineList, startTeg, finalTeg );
		if ( values.size() != 0 ) {
			return values;
		}
		return null;
	}
	
	//получаем внутренности тега(ов)
	public static ArrayList<String> getValue( ArrayList<String> list, String startTeg, String finalTeg ) {
		ArrayList<String> fromTag = new ArrayList<String>();
		String text = "";
		boolean search = false;
		for ( int i = 0; i < list.size(); i++ ) {
			String line = list.get( i );
			for ( int i2 = 0; i2 < line.length(); i2++ ) {
				if ( i2 + startTeg.length() <= line.length()) {
					String teg = line.substring( i2, i2 + startTeg.length());
					if ( teg.equals( startTeg ) == true ) {
						i2 += startTeg.length();
						search = true;
					}
				}
				if ( i2 + finalTeg.length() <= line.length()) {
					String teg = line.substring( i2, i2 + finalTeg.length());
					if ( teg.equals( finalTeg ) == true ) {
						search = false;
						fromTag.add( text );
						text = "";
					}
				}
				if (( search == true ) & ( i2 < line.length())) {
					text += line.charAt( i2 );
				}
			}
		}
		return fromTag;
	}
	
	//получаем html страницу
	public static ArrayList<String> getSheet( HttpURLConnection value ) {
		ArrayList<String> sheet = new ArrayList<String>();
		try {
			BufferedReader sheetbuffer = new BufferedReader( new InputStreamReader( value.getInputStream()));
			String line = sheetbuffer.readLine();
			while ( line != null ) {
				sheet.add( line );
				line = sheetbuffer.readLine();
			}
			sheetbuffer.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return sheet;
	}
	
	//получение текущей даты
	public static String getDate() {
		Calendar calendar = Calendar.getInstance();
		
		int day = calendar.get( Calendar.DAY_OF_MONTH );
		String dayStr = "";
		if ( day < 10 ) {
			dayStr = "0" + Integer.toString( day );
		} else {
			dayStr = Integer.toString( day );
		}
		
		int month = calendar.get( Calendar.MONTH ) + 1;
		String monthStr = "";
		if ( month < 10 ) {
			monthStr = "0" + Integer.toString( month );
		} else {
			monthStr = Integer.toString( month );
		}
		
		int yearStr = calendar.get( Calendar.YEAR );
		
		return yearStr + monthStr + dayStr;
	}
}