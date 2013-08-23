package edu.jhu.hlt.concrete.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import edu.jhu.hlt.concrete.Concrete;
import edu.jhu.hlt.concrete.Concrete.Communication;
import edu.jhu.hlt.concrete.Concrete.Communication.Builder;
import edu.jhu.hlt.concrete.Concrete.EmailAddress;
import edu.jhu.hlt.concrete.Concrete.EmailCommunicationInfo;
import edu.jhu.hlt.concrete.Concrete.EntityMention;
import edu.jhu.hlt.concrete.Concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.Concrete.KeyValues;
import edu.jhu.hlt.concrete.Concrete.Section;
import edu.jhu.hlt.concrete.Concrete.Section.Kind;
import edu.jhu.hlt.concrete.Concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Concrete.Sentence;
import edu.jhu.hlt.concrete.Concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.Concrete.Situation;
import edu.jhu.hlt.concrete.Concrete.SituationMention;
import edu.jhu.hlt.concrete.Concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.Concrete.TextSpan;
import edu.jhu.hlt.concrete.Concrete.Token;
import edu.jhu.hlt.concrete.Concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.Concrete.Tokenization;
import edu.jhu.hlt.concrete.Concrete.UUID;
import edu.jhu.hlt.concrete.util.JsonUtil.JsonCommunication.JsonKeyValues;

import edu.jhu.hlt.concrete.io.ProtocolBufferReader;
import edu.jhu.hlt.concrete.io.ProtocolBufferWriter;
import groovyjarjarcommonscli.CommandLine;
import groovyjarjarcommonscli.CommandLineParser;
import groovyjarjarcommonscli.HelpFormatter;
import groovyjarjarcommonscli.Option;
import groovyjarjarcommonscli.Options;
import groovyjarjarcommonscli.PosixParser;
import groovyjarjarcommonscli.ParseException;
//import edu.jhu.hlt.concrete.util.JsonUtil.JsonCommunication.JsonKeyValues;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
//import com.google.gson.reflect.Type;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;



/**
 * The purpose of this utility is to provide a json wrapper class
 * and utility functions
 * for the protocol buffer Communication class.
 * 
 * Set test(String[] args) for an example of deserializing and serializing
 * Concrete Communication objects to and form the JsonCommunication wrapper class.
 * 
 * @author Tad, taylor.turpen@gmail.com
 * 
 */
public class JsonUtil {
	final static String NO_MESSAGE_ID_FLAG = "NO_MESSAGE_ID";
	
	public static class JsonCommunication {
		/*
		 * A JSON wrapper for concrete communication.
		 * 
		 */

		/*
		 * Subclasses
		 */
		private class UtilSectionSegmentation{
			//Body in the intuitive sense as a sequence
			//of sections
			private List<SegmentSection> sections;
			
			public List<SegmentSection> getSections() {
				return sections;
			}
			public void setSections(List<SegmentSection> sections) {
				this.sections = sections;
			}
			public UtilSectionSegmentation(List<SegmentSection> sects){
				this.sections = sects;
			}
			public void setAllParagraphs(List<SegmentSection> sects){
				this.sections = sects;
			}
			public void addAllParagraphs(List<SegmentSection> sects){
				for(SegmentSection sect: sects){
					this.sections.add(sect);
				}
			}
			public void addParagraph(SegmentSection sect){
				this.sections.add(sect);
			}
		}
		
		private class SegmentSection{
			//Section in the intuitive sense as a sequence
			//of sentences
			private String sectionText;
			private String kind;
			private List<SegmentSectionSentence> sentences;			
			
			public SegmentSection(String t, String k){
				this.sectionText = t;
				this.kind = k;
			}
			
			public SegmentSection(String substring, Kind kind2) {
				this.sectionText = substring;
				this.kind = kind2.name();
			}

			public void setKind(String kind){
				this.kind = kind;
			}
			
			public String getKind(){return kind;}
			

			public TextSpan getTextSpan(String rawText) {
				int start = rawText.indexOf(this.sectionText);
				int end = start + sectionText.length();
				TextSpan ts = TextSpan.newBuilder()
						.setStart(start)
						.setEnd(end)
						.build();
				return ts;
			}

			public List<SegmentSectionSentence> getSentences() {
				if (this.sentences != null){return this.sentences;}
				return new ArrayList<SegmentSectionSentence>();
			}

			public Kind getSectionKind() {
				if(this.kind.contentEquals("METADATA")){return Concrete.Section.Kind.METADATA;}
				else if(this.kind.contentEquals("PASSAGE")){return Concrete.Section.Kind.PASSAGE;}
				else if(this.kind.contentEquals("IMAGE")){return Concrete.Section.Kind.IMAGE;}
				else if(this.kind.contentEquals("LIST")){return Concrete.Section.Kind.LIST;}
				else if(this.kind.contentEquals("TABLE")){return Concrete.Section.Kind.TABLE;}
				else {return Concrete.Section.Kind.OTHER;}
			}
		}
		
		private class EventSpan{			
			//A string for the type of event
			private String eventType;
			
			private List<ArgumentSpan> arguments;
			
			public EventSpan(String eventType){
				this.eventType = eventType;
				arguments = new ArrayList<ArgumentSpan>();
			}
		}
		
		private class ArgumentSpan{
			private String role;
			private List<Span> spans;
			
			public ArgumentSpan(String role){
				this.role = role;
				spans = new ArrayList<Span>();
			}
		}
		
		private class Span{
			private int start;
			private int end;
			public Span(int start, int end){
				this.start = start; 
				this.end = end;
			}
		}
		
		
		private class SegmentSectionSentence{
			//Sentence as the atomic unit of the JsonCommunication
			//Not currently supported
			//Class
			private String text = "";
			public void setText(String text){
				this.text = text;
			}
			public SegmentSectionSentence(){
				this.text = "";
			}
		}	
		
		public class JsonKeyValues{
			private String key;
			private List<String> values;
			
			public JsonKeyValues(String key, List<String> valuesList) {
				this.key = key;
				this.values = valuesList;
			}
			public KeyValues getConcreteKeyValues(){
				return KeyValues.newBuilder()
						.setKey(this.key)
						.addAllValues(this.values)
						.build();
			}
		}
		/*
		 * Constants
		 */

		
		/*
		 * Generic meta-information for communication
		 */
		private long startTime;
		private String author;
		private String title;
		private String kind;
		
		public String getKind() {
			return kind;
		}
		
		public Communication.Kind getKindCommunication() {
			if (kind.contentEquals("EMAIL")){ return Communication.Kind.EMAIL;}
			else if (kind.contentEquals("NEWS")){ return Communication.Kind.NEWS;}
			else if (kind.contentEquals("WIKIPEDIA")){ return Communication.Kind.WIKIPEDIA;}
			else if (kind.contentEquals("TWEET")){ return Communication.Kind.TWEET;}
			else if (kind.contentEquals("PHONE_CALL")){ return Communication.Kind.PHONE_CALL;}
			else if (kind.contentEquals("USENET")){ return Communication.Kind.USENET;}
			else if (kind.contentEquals("BLOG")){ return Communication.Kind.BLOG;}			
			else { return Communication.Kind.OTHER;}
		}


		public void setKind(String kind) {
			this.kind = kind;
		}


		/*
		 * Email Headers
		 */
		private String messageId;
		private String senderEmail;
		private List<String> recipientsTo = new ArrayList<String>();
		private List<String> recipientsCc = new ArrayList<String>();
		private List<String> recipientsBcc = new ArrayList<String>();
		
		/*
		 * Content
		 */
		private String rawText;//The raw byte->String of the email
		private String bodyText;//The byte->String->Mime message->getText(Mime message)
		private List<UtilSectionSegmentation> segmentations = new ArrayList<UtilSectionSegmentation>();//A list of the email messages contained in emailBodyText	
		private List<JsonKeyValues> metadata = new ArrayList<JsonKeyValues>();

		private List<EventSpan> eventSpans = new ArrayList<EventSpan>(); // A list of the events in the text, grounded in character spans 
		
		public static List<String> getAcceptedKeys(){
			List<String> acceptedKeys = new ArrayList <String>();
			acceptedKeys.add("rawText");
			acceptedKeys.add("bodyText");
			acceptedKeys.add("segmentations");
			acceptedKeys.add("author");
			acceptedKeys.add("title");
			acceptedKeys.add("startTime");
			acceptedKeys.add("metadata");
			acceptedKeys.add("recipientsTo");
			acceptedKeys.add("recipientsCc");
			acceptedKeys.add("recipientsBcc");
			acceptedKeys.add("messageId");
			acceptedKeys.add("kind");
			acceptedKeys.add("eventSpans");
			return acceptedKeys;
		}
		
		
		public List<JsonKeyValues> getMetadata() {
			return metadata;
		}

		public void addJKVMetadata(JsonKeyValues jkv){
			this.metadata.add(jkv);
		}

		public void addMetadata(KeyValues kvs) {
			this.metadata.add(new JsonKeyValues(kvs.getKey(),kvs.getValuesList()));
		}

		/*
		 * Manipulation methods
		 */
		public JsonCommunication(){
			this.rawText = "";
		}
				
				
		/*
		public JsonCommunication(String startTime, String rawText) throws ParseException{
			this.rawText = rawText;
			//Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTime);
			this.startTime = date.getTime();
		}*/
		
		/*
		/**
		 * Convert a SectionSegmentation to a Body object
		 * @param seg
		 * @param rawText
		 * @return the Body object that represents the rawText
		 *
		private Body segmentToBody(SectionSegmentation seg,String rawText) {
			List<Section> sections = seg.getSectionList();
			List<BodySection> bodySections = new ArrayList<BodySection>();			
			for(Section sec: sections){
				TextSpan ts = sec.getTextSpan();
				bodySections.add(new BodySection(rawText.substring(ts.getStart(),ts.getEnd()),sec.getKind()));
			}
			return new Body(bodySections);
		}*/
		
		/**
		 * Convert a Section to a Body object
		 * @param seg
		 * @param rawText
		 * @return the Body object that represents the rawText
		 */
		private UtilSectionSegmentation sectionsToUtilSectionSegmentation(List<Section> sections,String rawText) {
			//List<Section> sections = seg.getSectionList();
			List<SegmentSection> textSections = new ArrayList<SegmentSection>();			
			for(Section sec: sections){
				TextSpan ts = sec.getTextSpan();
				textSections.add(new SegmentSection(rawText.substring(ts.getStart(),ts.getEnd()),sec.getKind()));
			}
			return new UtilSectionSegmentation(textSections);
		}
		
		/**
		 * Convert a SectionSegmentation to a Body object
		 * @param seg
		 * @param rawText
		 * @return the Body object that represents the rawText
		 */
		private SectionSegmentation emailBodyToSegment(UtilSectionSegmentation body,String rawText) {
			//List<Section> sections = seg.getSectionList();
			List<SegmentSection> bodySections = body.getSections();
			
			List<Section> sections = new ArrayList<Section>();			
			for(SegmentSection bodySec: bodySections){
				TextSpan ts = bodySec.getTextSpan(rawText);
				sections.add(Concrete.Section.newBuilder()
						.setTextSpan(ts)
						.setKind(bodySec.getSectionKind())
						.setUuid(IdUtil.generateUUID())
						.build());
				//bodySections.add(new BodySection(rawText.substring(ts.getStart(),ts.getEnd()),bodySec.getKind()));
			}
			return SectionSegmentation.newBuilder()
					.addAllSection(sections)
					.setUuid(IdUtil.generateUUID())
					.build();
		}
	
		/*
		 * Accessor methods
		 * 
		 */
		
		public long getStartTime() {
			return startTime;
		}

		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		
		public void setStartTime(String string) {
			this.startTime = Long.parseLong(string);
		}
		
		private void setAuthor(EmailAddress senderAddress) {
			this.author = senderAddress.getAddress();			
		}

		private void setTitle(String messageId2) {
			this.title = messageId2;			
		}
		
		public String getMessageId() {
			if(messageId != null){return messageId;}
			return JsonUtil.NO_MESSAGE_ID_FLAG;
		}
		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}
		public String getSenderEmail() {
			return senderEmail;
		}
		public void setSenderEmail(String senderEmail) {
			this.senderEmail = senderEmail;
		}
		public List<String> getRecipientsTo() {
			return recipientsTo;
		}
		public void addRecipientsTo(List<EmailAddress> list) {
			for(EmailAddress ea: list){
				if(ea.hasAddress()){
					this.recipientsTo.add(ea.getAddress());
				}
			}
		}
		
		public void addRecipientsBcc(List<EmailAddress> list) {
			for(EmailAddress ea: list){
				if(ea.hasAddress()){
					this.recipientsBcc.add(ea.getAddress());
				}
			}
		}
		
		public void addRecipientsCc(List<EmailAddress> list) {
			for(EmailAddress ea: list){
				if(ea.hasAddress()){
					this.recipientsTo.add(ea.getAddress());
				}
			}
		}
		
		public List<String> setRecipientsCc() {
			return recipientsCc;
		}
		
		public List<EmailAddress> getConcreteRecipientsCc() {
			List<EmailAddress> list = new ArrayList<EmailAddress>();
			for(String s: recipientsCc){
				list.add(EmailAddress.newBuilder().setAddress(s).build());
			}
			return list;
		}
		
		public List<EmailAddress> getConcreteRecipientsBcc() {
			List<EmailAddress> list = new ArrayList<EmailAddress>();
			for(String s: recipientsBcc){
				list.add(EmailAddress.newBuilder().setAddress(s).build());
			}
			return list;
		}
		
		public List<EmailAddress> getConcreteRecipientsTo() {
			List<EmailAddress> list = new ArrayList<EmailAddress>();
			for(String s: recipientsTo){
				list.add(EmailAddress.newBuilder().setAddress(s).build());
			}
			return list;
		}
		
		public EmailAddress getConcreteAuthor() {
			if(this.author != null){
				return EmailAddress.newBuilder().setAddress(this.author).build();
			}
			return EmailAddress.newBuilder().setAddress("").build();
		}
		
		public void setRecipientsCc(List<String> recipientsCc) {
			this.recipientsCc = recipientsCc;
		}
		
		public List<String> getRecipientsBcc() {
			return recipientsBcc;
		}
		
		public void setRecipientsBcc(List<String> recipientsBcc) {
			this.recipientsBcc = recipientsBcc;
		}
		public String getRawText() {
			return rawText;
		}
		public void setRawText(String rawText) {
			this.rawText = rawText;
		}
		public String getBodyText() {
			return bodyText;
		}
		public void setBodyText(String emailBodyText) {
			this.bodyText = emailBodyText;
		}
		
		public void setBodyText() {
			String text = "";
			for(UtilSectionSegmentation b: this.segmentations){
				for(SegmentSection bs: b.sections){
					text += bs.sectionText+"\n";
				}
			}
			this.bodyText = text;
		}
		
		public List<UtilSectionSegmentation> getBodyChain() {
			return segmentations;
		}
		public void setBodyChain(List<UtilSectionSegmentation> emailChain) {
			this.segmentations = emailChain;
		}
		public void setBodyChain(UtilSectionSegmentation body) {
			this.segmentations = new ArrayList<UtilSectionSegmentation>();
			this.segmentations.add(body);
		}
		public void addSectionToChain(List<Section> sec, String rawText){
			UtilSectionSegmentation body = sectionsToUtilSectionSegmentation(sec,rawText);
			this.segmentations.add(body);
		}
		public JsonObject toJsonObject(){
			Gson gson = new Gson();
			return gson.toJsonTree(this).getAsJsonObject();
		}
		public JsonElement toJsonElement(){
			Gson gson = new Gson();
			return gson.toJsonTree(this).getAsJsonObject();
		}
		public void addMetadata(String key, List<String> values) {
			JsonKeyValues jkv = new JsonKeyValues(key,values);
			this.metadata.add(jkv);			
		}



		/**
		 * Take a given SituationMention, and add an EventSpan to the JsonCommunication 
		 * that represents it.
		 * 
		 * @param situationMention
		 * @param communication 
		 */
		public void addEventSpans(SituationMention situationMention, Communication communication) {
			if (Situation.Type.EVENT.equals(situationMention.getSituationType())){
				EventSpan eventSpan = new EventSpan("SitKindLemma"+(Math.random()*10));
				for (SituationMention.Argument argument : situationMention.getArgumentList()){
					ArgumentSpan argumentSpan = new ArgumentSpan(argument.getRoleLabel());
					
					//Only handle entity mention arguments
					if (argument.hasEntityMentionId()){
						EntityMention entityMention = findEntityMention(argument.getEntityMentionId(), communication);
						TokenRefSequence tokenRefSequence = entityMention.getTokens();
						Tokenization tokenization = findTokenization(tokenRefSequence.getTokenizationId(), communication);
						for (int tokenIndex : tokenRefSequence.getTokenIndexList()){
							Token token = findToken(tokenization, tokenIndex);
							Span span = new Span(token.getTextSpan().getStart(), token.getTextSpan().getEnd());
							argumentSpan.spans.add(span);
						}
					}
					eventSpan.arguments.add(argumentSpan);
				}
				eventSpans.add(eventSpan);
			}
		}
		
		/**
		 * Given an entity mention UUID, find the entity mention on the communication
		 * with the matching id.
		 * 
		 * @param value
		 * @param communication
		 * @return
		 */
		private EntityMention findEntityMention(UUID value,
				Communication communication) {
			for (EntityMentionSet entityMentionSet : communication.getEntityMentionSetList()){
				for (EntityMention entityMention : entityMentionSet.getMentionList()){
					if (entityMention.getUuid().equals(value)){
						return entityMention;
					}
				}
			}
			return null;
		}


		/**
		 * Within a communication, find a tokenization that has the given tokenization UUID
		 * 
		 * @param tokenizationId
		 * @param communication
		 * @return
		 */
		private Tokenization findTokenization(UUID tokenizationId,
				Communication communication) {
			for (SectionSegmentation sectionSegmentation : communication.getSectionSegmentationList()){
				for (Section section : sectionSegmentation.getSectionList()){
					for (SentenceSegmentation sentenceSegmentation : section.getSentenceSegmentationList()){
						for (Sentence sentence : sentenceSegmentation.getSentenceList()){
							for (Tokenization tokenization : sentence.getTokenizationList()){
								if (tokenization.getUuid().equals(tokenizationId)){
									return tokenization;
								}
							}
						}
					}
				}
			}
			return null;
		}


		/**
		 * Take a tokenization and a token id, and return the token withtin the tokenization
		 * that has the specified id 
		 * 
		 * @param tokenization
		 * @param tokenIndex
		 * @return
		 */
		private Token findToken(Tokenization tokenization, int tokenIndex) {
			for (Token token : tokenization.getTokenList()){
				if (token.getTokenIndex() == tokenIndex){
					return token;
				}
			}
			return null;
		}


		public void setMetadata(List<JsonKeyValues> metadata) {
			this.metadata = metadata;
		}	
		
		public void setAuthor(String string) {
			this.author = string;			
		}


		public List<UtilSectionSegmentation> getListOfSectionsAsBodyChain(
				Object value) {
			Type listType = new TypeToken<List<UtilSectionSegmentation>>(){}.getType();
			JsonArray ja = (JsonArray)value;
			
			Gson gson = new Gson();
			return gson.fromJson(ja, listType);			
		}
		
		public List<JsonKeyValues> getPriorMetadataAsKeyValuePairs(
				Object value) {
			Type listType = new TypeToken<List<JsonKeyValues>>(){}.getType();
			JsonArray ja = (JsonArray)value;			
			Gson gson = new Gson();
			return gson.fromJson(ja, listType);			
		}


		public void addMetadata(List<JsonKeyValues> metadataList) {
			for(JsonKeyValues jkv: metadataList){
				this.metadata.add(jkv);
			}			
		}


		public Iterable<? extends SectionSegmentation> getConcreteEmailSectionSegmentation() {
			List<SectionSegmentation> sectSeg = new ArrayList<SectionSegmentation>();
			for(UtilSectionSegmentation b: segmentations){
				sectSeg.add(emailBodyToSegment(b,this.rawText));
			}
			return sectSeg;
		}


		public List<String> getSents() {
			List<String> sents = new ArrayList<String>();
			for(UtilSectionSegmentation b: this.segmentations){
				for(SegmentSection bs: b.sections){
					if(bs.getKind().contentEquals("PASSAGE")){
						for(SegmentSectionSentence bss: bs.getSentences()){
							sents.add(bss.text);
						}
					}
				}
			}
			return sents;
		}

		public List<KeyValues> getConcreteMetadata() {
			List<KeyValues> kvs = new ArrayList<KeyValues>();
			for(JsonKeyValues jkv : this.metadata){
				kvs.add(jkv.getConcreteKeyValues());
			}
			return kvs;
		}

		public Iterable<? extends SituationMentionSet> getSituationMentionSet() {
			//I don't know what the EventSpans look like, I would need some conrete objects with EventSpans
			//to finish this code
			List<EventSpan> events = this.eventSpans;
			for(EventSpan es : events){
				String type = es.eventType;
				List<ArgumentSpan> arguments = es.arguments;
				for(ArgumentSpan a : arguments){
					String role = a.role;
					List<Span> spans = a.spans;
					for(Span s : spans){
						int start = s.start;
						int end = s.end;
					}
				}
				
			}
			return null;
		}
		public void addStringRecipientsTo(List<String> values) {
			for (String v : values){
				if (v != null){	this.recipientsTo.add(v);}
			}
		}
		public void addStringRecipientsCc(List<String> values) {
			for (String v : values){
				if (v != null){	this.recipientsCc.add(v);}
			}
		}
		public void addStringRecipientsBcc(List<String> values) {
			for (String v : values){
				if (v != null){	this.recipientsBcc.add(v);}
			}
		}

	}
	/*
	 * Handler Methods
	 */
	
	/**
	 * Given a Communication object return the JsonCommunication jsonObject
	 * @input commIn Given a Communiation object, convert 
	 * 					specific data members and return the json object
	 * 
	 * @return the json object
	 */
	public static JsonObject toJson(Communication commIn) {
		JsonCommunication jcomm = toJsonCommunication(commIn);
		Gson gson = new Gson();
		JsonParser parser = new JsonParser();
		JsonObject jo = (JsonObject)parser.parse(gson.toJson(jcomm));
		return jo;
	}
	
	/**
	 * Given a json string representation of a JsonCommunication object
	 * return the JsonObject equivalent
	 * 
	 *@param json a string representation of a JsonCommunication Object
	 *
	 *@return a JsonObject of the JsonCommunication
	 */
	public static JsonObject toJsonObjectFromJsonString(String json) {
		Gson gson = new Gson();
		JsonCommunication jc = gson.fromJson(json,JsonCommunication.class);
		return jc.toJsonObject();
	}
	
	/**
	 * Given a communication, return the json string representation
	 * 
	 * @param commIn the Communication input
	 * 
	 * @return the json String representation of the JsonCommunication
	 */
	public static String toJsonString(Communication commIn){
		Gson gson = new Gson();
		JsonCommunication jcomm = toJsonCommunication(commIn);
		return gson.toJson(jcomm);
	}
	
	/**
	 * If the Json string is already in perfect JsonCommunication form,
	 * return the JsonCommunication. If it is just a json string, not yet
	 * in JsonCommunication form, use toJsonCommunicationFromUnknown.
	 * 
	 *@param json a string representation of a JsonCommunication Object
	 *
	 *@return : a JsonCommunication given the json string
	 */
	public static JsonCommunication toJsonCommunicationFromWellFormed(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json,JsonCommunication.class);		
	}
	
	/**
	 * Given a Communication object, convert the object to a JsonCommunication 
	 * @param commIn the Concrete Communication object
	 * 
	 * @return the JsonCommunication object
	 */
	public static JsonCommunication toJsonCommunication(Communication commIn){
		JsonCommunication jcomm = new JsonCommunication();		
		
		//Get data members
		String rawText = commIn.getText();

		List<SectionSegmentation> segs = commIn.getSectionSegmentationList();
		if (segs.size() > 0){
			//Get first sectionsegmentation hypothesis
			List<Section> sections = segs.get(0).getSectionList();
			jcomm.setBodyChain(jcomm.sectionsToUtilSectionSegmentation(sections,rawText));
		}
		//SectionSegmentation headers = segs.get(0);//Header text is written as first secseg
		
		List<KeyValues> metadata = commIn.getMetadataList();
		for(KeyValues kvs : metadata){
			jcomm.addMetadata(kvs);
		}
		
		//Set data members
		jcomm.setRawText(rawText);
		jcomm.setKind(commIn.getKind().toString());
		if(commIn.getKind().toString().contentEquals("EMAIL")){
			EmailCommunicationInfo info = commIn.getEmailInfo();
			jcomm.setMessageId(info.getMessageId());
			jcomm.setAuthor(info.getSenderAddress());
			jcomm.setTitle(info.getMessageId());
			jcomm.addRecipientsTo(info.getToAddressList());
			jcomm.addRecipientsBcc(info.getBccAddressList());
			jcomm.addRecipientsCc(info.getCcAddressList());
		}
		jcomm.setBodyText();
		
		for (SituationMentionSet situationMentionSet : commIn.getSituationMentionSetList()){
			for (SituationMention situationMention : situationMentionSet.getMentionList()){
				jcomm.addEventSpans(situationMention, commIn);
			}
		}
		
		
		for (SituationMentionSet situationMentionSet : commIn.getSituationMentionSetList()){
			for (SituationMention situationMention : situationMentionSet.getMentionList()){
				jcomm.addEventSpans(situationMention, commIn);
			}
		}
		
		return jcomm;		
	}
	
	/**
	 * Evaluate the key and value fields of the json
	 * object, find the ones that aren't included and add them as metadata
	 * 
	 * @param json
	 * @return
	 */
	public static JsonCommunication toJsonCommunicationFromUnknown(String json,
			boolean validate){
		Gson gson = new Gson();
		JsonParser jp = new JsonParser();
		JsonObject jo = (JsonObject)jp.parse(json);
		/*If we knew this was a well formed JsonCommunication:*/		
		//JsonCommunication jcomm = gson.fromJson(jo, JsonCommunication.class);
		//jcomm.getStartTime();
		//return jcomm;
				
		//But we don't know that it's well formed, so we iterate through the fields	
		Set<Entry<String, JsonElement>> es = jo.entrySet();
		String key; List<String> values;
		JsonCommunication jcomm = new JsonCommunication();
		List<String> acceptedKeys = jcomm.getAcceptedKeys();
		List<String> validKeys = new ArrayList<String>();
		List<String> invalidKeys = new ArrayList<String>();
		JsonArray ja;
		//List<JsonKeyValues> metadata = jcomm.getMetadata();
		
		for(Entry e: es){
			key = (String)e.getKey();
			if (acceptedKeys.contains(key)){
				if(key.contentEquals("segmentations")){
					//Given a list of segmentations, add them as a chain of texts
					if(validate){validKeys.add(key);}
					ja = (JsonArray)e.getValue();
					jcomm.setBodyChain(jcomm.getListOfSectionsAsBodyChain(e.getValue()));
				}
				else if(key.contentEquals("metadata")){
					jcomm.addMetadata(jcomm.getPriorMetadataAsKeyValuePairs(e.getValue()));
				}
				//Talk to Dave about event spans so I can finish this code
				//else if(key.contentEquals("eventSpans")){
				//	jcomm.addEventSpans(jcomm.get, communication)
				//}
				else{
					//Else extract the values
					values = fromJsonStringToValueStringList(e.getValue());//(List<String>)e.getValue();
					if(key.contentEquals("rawText")){
						jcomm.setRawText(values.get(0));
						if(validate){ validKeys.add(key);}
					}
					else if(key.contentEquals("startTime")){
						jcomm.setStartTime(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("author")){
						jcomm.setAuthor(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("title")){
						jcomm.setTitle(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("bodyText")){
						jcomm.setBodyText(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("messageId")){
						jcomm.setMessageId(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("kind")){
						jcomm.setKind(values.get(0));
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("recipientsTo")){
						if (values.size() > 0){ jcomm.addStringRecipientsTo(values);}
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("recipientsCc")){
						if (values.size() > 0){jcomm.addStringRecipientsCc(values);}
						if(validate){ validKeys.add(key);};
					}
					else if(key.contentEquals("recipientsBcc")){
						if (values.size() > 0){	jcomm.addStringRecipientsBcc(values);}
						if(validate){ validKeys.add(key);};
					}
				}
			}
			else{
				//Key not in valid key list
				values = fromJsonStringToValueStringList(e.getValue());//(List<String>)e.getValue();
				if(validate){invalidKeys.add(key);}
				jcomm.addMetadata(key,values);	
			}			
		}
		if (validate){
			System.out.println("Valid Keys:");
			for(String s: validKeys){
				System.out.println("\t"+s);
			}
			System.out.println("Would be successfully ingested into Concrete");
			System.out.println("InValid Keys:");
			for(String s: invalidKeys){
				System.out.println("\t"+s);
			}
			System.out.println("Would be added to the concrete object as Metadata");
		}
		return jcomm;
	}
	
	
	/**
	 * Takes a value of unknown type, sets the type and returns the appropriate
	 * String list
	 * 
	 * @param value An unknown value of some Json type
	 * @param object 
	 * 
	 * @return returns a string list so it can be handled as a set of values
	 */
	private static List<String> fromJsonStringToValueStringList(Object value) {
		Class cl = value.getClass();
		JsonPrimitive jp;
		Gson gson = new Gson();
		Type listType = new TypeToken<List<String>>(){}.getType();
		List<String> result = new ArrayList<String>();

		if (cl.equals(JsonPrimitive.class)){
			jp = (JsonPrimitive)value;
			result.add(jp.getAsString());
		}
		else if(cl.equals(JsonArray.class)){
			//Json array support not currently implemented
			String s = gson.toJson(value);
			try{
				result = gson.fromJson(s, listType);
			}
			catch (Exception e){
				System.err.println("Unable to parse:"+value+
						". Make sure you are using flat lists, embedded arrays not supported.");
			}
		}
		return result;
	}

	/**
	 * Get a list of communications as JsonObjects from
	 * a file of zipped communications.
	 * 
	 * @input filename the filename of the gzipped communications
	 * 
	 * @return the list of JsonObjects that represents the communications
	 * @throws Exception 
	 */
	public List<JsonObject> getJsonCommunicationsFromGzip(String filename) throws Exception{
		//InputStream in = new GZIPInputStream(new FileInputStream(filename));
		ProtocolBufferReader in = new ProtocolBufferReader(filename, Communication.class);
		List<JsonObject> jcomms = new ArrayList<JsonObject>();
		Communication commIn;
		
		//while((commIn = Communication.parseDelimitedFrom(in)) != null){
		while((commIn = (Communication)in.next()) != null){
			jcomms.add(toJson(commIn));
		}			
		in.close();
		return jcomms;
	}
	
	/**
	 * Get a list of strings that are the Json representation of the
	 * communication objects in the file filename.
	 * 
	 * @param filename the filename of the gzipped zerialized Concrete Communication objects
	 * 
	 * @return the String list of the json JsonCommunication objects
	 * @throws Exception 
	 */
	public List<String> getJsonStringsFromGzip(String filename) throws Exception{
		//InputStream in = new GZIPInputStream(new FileInputStream(filename));
		ProtocolBufferReader in = new ProtocolBufferReader(filename, Communication.class);
		List<String> jcomms = new ArrayList<String>();
		Communication commIn;		
		//while((commIn = Communication.parseDelimitedFrom(in)) != null){
		while(in.hasNext()){
			commIn = (Communication)in.next();
			if (commIn != null){
				jcomms.add(toJsonString(commIn));
			}
		}			
		in.close();
		return jcomms;
	}
	
	/**
	 * Get a list of communications as JsonObjects from
	 * a file of zipped communications.
	 * 
	 * @param filename the filename of the gzipped communications
	 * 
	 * @return jcomms the list of JsonObjects that represents the communications
	 */
	public List<JsonObject> getJsonCommunications(String filename) throws FileNotFoundException, IOException{
		InputStream in = new GZIPInputStream(new FileInputStream(filename));
		List<JsonObject> jcomms = new ArrayList<JsonObject>();
		Communication commIn;
		
		while((commIn = Communication.parseDelimitedFrom(in)) != null){
			jcomms.add(toJson(commIn));
		}			
		in.close();
		return jcomms;
	}
	
	/**
	 * Get a list of strings that are the Json representation of the
	 * communication objects in the file filename.
	 * 
	 * @param filename the filename of the serialized Concrete communication objects
	 * @return a String List of the Json JsonCommunication strings
	 *  
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<String> getJsonStrings(String filename) throws FileNotFoundException, IOException{
		InputStream in = new FileInputStream(filename);
		List<String> jcomms = new ArrayList<String>();
		Communication commIn;		
		while((commIn = Communication.parseDelimitedFrom(in)) != null){
			jcomms.add(toJsonString(commIn));
		}			
		in.close();
		return jcomms;
	}
	
	/*
	 * 
	 */
	/**
	 * Convert the JsonCommunication object to a protobuf Communication object
	 * 
	 * @param jcomm the JsonCommunication object
	 * @return the Communication object
	 */
	public static Communication toCommunication(JsonCommunication jcomm){
		Communication cb = new ProtoFactory().generateMockCommunication();
		Communication comm = cb.toBuilder()
				.setText(jcomm.getRawText())
				.setStartTime(jcomm.getStartTime())				
				.build();
		return comm;		
	}
	
	/**
	 * Convert the JsonCommunication object to a protobuf Communication object
	 * 
	 * @param jcomm the JsonCommunication object
	 * @return the Communication object
	 */
	public static Communication toConcreteEmail(JsonCommunication jcomm){
		Communication cb = new ProtoFactory().generateMockCommunication();
		Communication comm = cb.toBuilder()
				.setText(jcomm.getRawText())				
				.setStartTime(jcomm.getStartTime())
				.setEmailInfo(EmailCommunicationInfo.newBuilder()
					.setSenderAddress(jcomm.getConcreteAuthor())
					.setMessageId(jcomm.getMessageId())
					.addAllToAddress(jcomm.getConcreteRecipientsTo())
					.addAllBccAddress(jcomm.getConcreteRecipientsBcc())
					.addAllCcAddress(jcomm.getConcreteRecipientsCc())
					.build()
					)
				.addAllMetadata(jcomm.getConcreteMetadata())
				.addAllSectionSegmentation(jcomm.getConcreteEmailSectionSegmentation())
				.setKind(Communication.Kind.EMAIL)
				.build();
		return comm;		
	}
	
	/**
	 * Convert the JsonCommunication object to a protobuf Communication object
	 * 
	 * @param jcomm the JsonCommunication object
	 * @return the Communication object
	 */
	public static Communication toConcreteCommunication(JsonCommunication jcomm){
		Communication cb = new ProtoFactory().generateMockCommunication();
		Builder comm = cb.toBuilder()
				.setText(jcomm.getRawText())
				.setStartTime(jcomm.getStartTime());
		if (jcomm.getKind().contentEquals("EMAIL")){
			comm.setEmailInfo(EmailCommunicationInfo.newBuilder()
					.setSenderAddress(jcomm.getConcreteAuthor())
					.setMessageId(jcomm.getMessageId())
					.addAllToAddress(jcomm.getConcreteRecipientsTo())
					.addAllBccAddress(jcomm.getConcreteRecipientsBcc())
					.addAllCcAddress(jcomm.getConcreteRecipientsCc()));
		}
		comm.addAllMetadata(jcomm.getConcreteMetadata())
				.addAllSectionSegmentation(jcomm.getConcreteEmailSectionSegmentation())
				.setKind(jcomm.getKindCommunication())
				//.addAllSituationMentionSet(jcomm.getSituationMentionSet())
				.build();
		return comm.build();		
	}
	
	/**
	 * Given a string representation of a JsonCommunication
	 * convert the communication to the jcomm and return the
	 * protobuf communication object.
	 * 
	 * @param json A string reprsentation of a JsonCommunication
	 * 
	 * @return the Communication object
	 */
	public Communication toCommunication(String json){
		JsonCommunication jcomm = toJsonCommunicationFromWellFormed(json);
		return toCommunication(jcomm);
	}
	
	/**
	 * Save the concrete object to a file
	 * @param jcomm
	 * @param outFilename
	 * @throws IOException
	 */
	public static void saveConcrete(JsonCommunication jcomm, String outFilename) throws IOException{
		//File outputFile = new File(outFilename);
		ProtocolBufferWriter pbf = new ProtocolBufferWriter(outFilename);
		Communication comm = toConcreteCommunication(jcomm);//toConcreteEmail(jcomm);
		try{
			pbf.write(comm);
			pbf.close();
		}catch(IOException e){
			System.err.println("Could not write protobuf to "+ outFilename);
			e.printStackTrace();
		}
	}
	
	public static void saveConcreteFromJson(String json, String outFilename) throws IOException{
		JsonCommunication jcs = toJsonCommunicationFromUnknown(json,false);//Do not validate
		saveConcrete(jcs,outFilename);
	}

	
	public static void test(String[] args) {
		System.out.println("Testing JsonUtil");
		try {
			//To get FROM a communication file TO jsonObject
			JsonUtil ju = new JsonUtil();	
			String filename = args[0];
			boolean validate = false;
			
			List<JsonObject> jcomms = null;
			List<String> jcommstrings = null;
			jcomms = ju.getJsonCommunicationsFromGzip(filename);
			jcommstrings = ju.getJsonStringsFromGzip(filename);


			//To get FROM a communication file TO json string

			
			//To get TO a communication object FROM a json object
			Communication comm;
			Communication concreteEmail;
			Communication concreteEmailExtended;
			Gson gson = new Gson();
			
			for(JsonObject jcomm : jcomms){
				JsonCommunication jc = gson.fromJson(jcomm, JsonCommunication.class);
				comm = ju.toCommunication(jc);
			}
			
			//To get TO a communication object FROM a json string
			for(String jcomm : jcommstrings){
				//Changing string values improperly, may be my static handling, double check Monday
				JsonCommunication jc = toJsonCommunicationFromWellFormed(jcomm);
				JsonCommunication jcs = toJsonCommunicationFromUnknown(jcomm,validate);
				comm = ju.toCommunication(jc);
				concreteEmail = ju.toConcreteEmail(jc);
				concreteEmailExtended = ju.toConcreteEmail(jcs);
				saveConcreteFromJson(jcomm,"testSaveConcrete.out.gz");
				List<String> jsonstrings = ju.getJsonStringsFromGzip("testSaveConcrete.out.gz");
				System.out.println(comm.getStartTime());
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		// Set up command line arguments
		Options options = new Options();
		options.addOption(new Option("h", "help", false, "dislay this help and exit"));
		options.addOption(new Option("fc", "from-concrete", false,
				"Given a concrete file, print the json representation." ));
		options.addOption(new Option("tc", "to-concrete", false,
				"Given a json file (A file that contains a json string on the first line), save the concrete object equivalent." ));
		options.addOption(new Option("v", "validate", false,
				"If to-concrete then check the key/value pairs." ));
		options.addOption(new Option("js", "just-sentences", false,
				"Given a concrete file, return just the sentences in json format.(not yet implemented)" ));
		options.addOption(new Option("jp", "just-passages", false,
				"Given a concrete file, return just the sections marked as PASSAGE.(not yet implemented)" ));
		options.addOption(new Option("uuid", "uuid", false,
				"Specify the exact uuid of the Concrete object.(not yet implemented)" ));

		
		String usage = "JsonUtil Json utility for concrete.\n";
		HelpFormatter formatter = new HelpFormatter();
		String helpFooter = "JsonUtil handles concrete objects and converts them to and from json.\n"+ 
				"Specify the functions you would like to perform.\n" +
				"If going to Concrete, include a json filename and desired concrete filename for output.\n"+
				"If going from Concrete, include a concrete filename alone.\n";
		CommandLine line = null;
		String[] otherArgs = null;
		JsonUtil ju = new JsonUtil();
		
		boolean fromConcrete = true;
		boolean toConcrete = false;
		boolean justSentences = false;
		boolean justPassages = false;
		boolean validate = false;
		
		try{
			line = parser.parse(options, args);
			otherArgs = line.getArgs();
			// Handle help
			if (line.hasOption("help")) {
				formatter.printHelp(usage, "", options, helpFooter);
				System.exit(0);
			}
			if (line.hasOption("to-concrete")){
				toConcrete = true;
				fromConcrete = false;
			}
			if (line.hasOption("from-concrete")){
				if (toConcrete){ 
					System.err.println("Cannot convert from and also to Concrete");
					System.exit(0);					
				}
				fromConcrete = true;
			}
			if (line.hasOption("just-sentences")){
				justSentences = true;
			}
			if (line.hasOption("just-passages")){
				justPassages = true;
			}
			if (line.hasOption("validate")){
				validate = true;
			}		
		}catch(ParseException exp) {
			System.out.println(exp.getMessage());
			formatter.printHelp(usage, "", options, helpFooter);
			System.exit(64);
		}
		
		if (otherArgs.length < 1 || otherArgs.length > 2) {
			formatter.printHelp(usage, "", options, helpFooter);
			throw new ParseException("Incorrect number of required arguments specified");
		}
		
		final String in = otherArgs[0];
		
		if (toConcrete){
			if (otherArgs.length < 2){
				System.err.println("If going to Concrete, you must include a file containing "+
							"the desired json string and also a file to write to.");
				System.exit(0);
			}
			final String out = otherArgs[1];
			final Path outPath = Paths.get(out).toAbsolutePath();
			if (!outPath.toString().endsWith(".gz")){
				System.err.println("The concrete object name must contain .gz at the end for storage purposes.");
				System.exit(0);				
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in)));
			String jsonString = br.readLine();
			saveConcreteFromJson(jsonString,outPath.toString());			
		}
		else if (fromConcrete){
			final Path inPath = Paths.get(in).toAbsolutePath();
			List<String> jcommstrings = ju.getJsonStringsFromGzip(inPath.toString());
			JsonCommunication jcomm = null;
			if (justSentences){
				for(String s: jcommstrings){
					jcomm = toJsonCommunicationFromUnknown(s,validate);
					//System.out.print(jcomm.getSents());
					System.out.println(jcomm.bodyText);
				}
			}
			else{
				for(String s: jcommstrings){
					System.out.println(s);
				}
			}
		}	
	}
}
/*
 *Extended JSON support, not implemented, not functional yet
 
public static void merge(Entry entry, Builder builder){
//Detect if entry is array or not
Object value = entry.getValue();
Object key = (String)entry.getKey();
Gson gson = new Gson();
JsonElement je = gson.toJsonTree(value);
if(je.isJsonArray()){
	mergeArray(je,builder,gson);
}
else{
	je.getAsString();
}
//System.out.println("js:"+js+" je:"+je);
}

public static void mergeArray(JsonElement je, Builder builder, Gson gson){
JsonArray ja = je.getAsJsonArray();
for(JsonElement j: ja){
	if (j.isJsonArray()){mergeArray(j,builder,gson);}
	else{
		
	}
}		
}

public static void merge(JsonObject jobj, ExtensionRegistry ext, Communication.Builder builder){
String name = jobj.getAsString();
FieldDescriptor field;
Descriptor type = builder.getDescriptorForType();
field = type.findFieldByName(name);

ExtensionRegistry.ExtensionInfo extension = ext.findExtensionByName(name);
if (extension != null){
	if(extension.descriptor.getContainingType() != type){
		System.out.println("Extension "+name+"does not extend message type "+type.getFullName());
	}
	field = extension.descriptor;
}

handleValue(jobj,ext, builder,field,extension);

}

public static void handleValue(JsonObject jobj, ExtensionRegistry ext, Communication.Builder builder,
	FieldDescriptor field, ExtensionRegistry.ExtensionInfo extension){
Object value;
if(field.getJavaType() == FieldDescriptor.JavaType.MESSAGE){
	value = handleObject(jobj,ext, builder, field, extension);
}
}

public static Object handleObject(JsonElement jel, ExtensionRegistry ext, Communication.Builder builder,
	FieldDescriptor field, ExtensionRegistry.ExtensionInfo extension){
return null;
}
*/