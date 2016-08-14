package betterquesting.client.gui.editors;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import betterquesting.api.client.gui.INeedsRefresh;
import betterquesting.api.client.gui.IVolatileScreen;
import betterquesting.api.client.gui.premade.screens.GuiScreenThemed;
import betterquesting.api.enums.EnumLogic;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.enums.EnumSaveType;
import betterquesting.api.network.PacketTypeNative;
import betterquesting.api.quests.IQuestContainer;
import betterquesting.api.utils.IJsonStorage;
import betterquesting.api.utils.NBTConverter;
import betterquesting.client.gui.editors.json.GuiJsonObject;
import betterquesting.client.gui.misc.GuiBigTextField;
import betterquesting.client.gui.misc.GuiButtonQuesting;
import betterquesting.client.gui.misc.ITextEditor;
import betterquesting.network.PacketAssembly;
import betterquesting.network.PacketSender;
import betterquesting.quests.QuestDatabase;
import betterquesting.registry.ThemeRegistry;
import com.google.gson.JsonObject;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiQuestEditor extends GuiScreenThemed implements ITextEditor, IVolatileScreen, INeedsRefresh
{
	IJsonStorage<JsonObject> lastEdit;
	int id = -1;
	IQuestContainer quest;
	
	GuiTextField titleField;
	GuiBigTextField descField;
	
	public GuiQuestEditor(GuiScreen parent, IQuestContainer quest)
	{
		super(parent, I18n.format("betterquesting.title.edit_quest", I18n.format(quest.getUnlocalisedName())));
		this.quest = quest;
		this.id = QuestDatabase.INSTANCE.getKey(quest);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void initGui()
	{
		super.initGui();
		
		this.setTitle(I18n.format("betterquesting.title.edit_quest", I18n.format(quest.getUnlocalisedName())));
		
		if(lastEdit != null)
		{
			JsonObject prog = new JsonObject();
			quest.writeProgressToJSON(prog);
			quest.readFromJSON(lastEdit);
			quest.readProgressFromJSON(prog);
			lastEdit = null;
			SendChanges();
		}
		
		titleField = new GuiTextField(this.fontRendererObj, width/2 - 99, height/2 - 68 + 1, 198, 18);
		titleField.setMaxStringLength(Integer.MAX_VALUE);
		titleField.setText(quest.getUnlocalisedName());
		
		descField = new GuiBigTextField(this.fontRendererObj, width/2 - 99, height/2 - 28 + 1, 198, 18).enableBigEdit(this, 0);
		descField.setMaxStringLength(Integer.MAX_VALUE);
		descField.setText(quest.getUnlocalisedDescription());
		
		GuiButtonQuesting btn = new GuiButtonQuesting(1, width/2, height/2 + 28, 100, 20, I18n.format("betterquesting.btn.rewards"));
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(2, width/2 - 100, height/2 + 28, 100, 20, I18n.format("betterquesting.btn.tasks"));
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(3, width/2 - 100, height/2 + 48, 100, 20, I18n.format("betterquesting.btn.requirements"));
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(4, width/2, height/2 + 68, 100, 20, I18n.format("betterquesting.btn.advanced"));
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(5, width/2 - 100, height/2 + 8, 200, 20, I18n.format("betterquesting.btn.is_main") + ": " + quest.isMain());
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(6, width/2, height/2 + 48, 100, 20, I18n.format("betterquesting.btn.logic") + ": " + quest.logic);
		this.buttonList.add(btn);
		btn = new GuiButtonQuesting(7, width/2 - 100, height/2 + 68, 100, 20, I18n.format("betterquesting.btn.show") + ": " + quest.getVisibility().toString());
		this.buttonList.add(btn);
	}
	
	@Override
	public void refreshGui()
	{
		this.quest = QuestDatabase.INSTANCE.getValue(id);
		
		if(quest == null)
		{
			mc.displayGuiScreen(parent);
			return;
		}
	}
	
	@Override
	public void drawScreen(int mx, int my, float partialTick)
	{
		super.drawScreen(mx, my, partialTick);
		
		if(QuestDatabase.updateUI)
		{
			QuestDatabase.updateUI = false;
			lastEdit = null;
			initGui();
		}
		
		titleField.drawTextBox();
		descField.drawTextBox();

		mc.fontRenderer.drawString(I18n.format("betterquesting.gui.name"), width/2 - 100, height/2 - 80, getTextColor(), false);
		mc.fontRenderer.drawString(I18n.format("betterquesting.gui.description"), width/2 - 100, height/2 - 40, getTextColor(), false);
	}
	
	@Override
	public void actionPerformed(GuiButton button)
	{
		super.actionPerformed(button);
		
		if(button.id == 1) // Rewards
		{
			mc.displayGuiScreen(new GuiRewardEditor(this, quest));
		} else if(button.id == 2) // Tasks
		{
			mc.displayGuiScreen(new GuiTaskEditor(this, quest));
		} else if(button.id == 3) // Prerequisites
		{
			mc.displayGuiScreen(new GuiPrerequisiteEditor(this, quest));
		} else if(button.id == 4) // Raw JSON
		{
			this.lastEdit = new JsonObject();
			quest.writeToJSON(lastEdit);
			mc.displayGuiScreen(new GuiJsonObject(this, lastEdit));
		} else if(button.id == 5)
		{
			quest.isMain = !quest.isMain;
			button.displayString = I18n.format("betterquesting.btn.is_main") + ": " + quest.isMain;
			SendChanges();
		} else if(button.id == 6)
		{
			EnumLogic[] logic = EnumLogic.values();
			quest.logic = logic[(quest.logic.ordinal() + 1)%logic.length];
			button.displayString = I18n.format("betterquesting.btn.logic") + ": " + quest.logic;
			SendChanges();
		} else if(button.id == 7)
		{
			EnumQuestVisibility[] vis = EnumQuestVisibility.values();
			quest.visibility = vis[(quest.visibility.ordinal() + 1)%vis.length];
			button.displayString =  I18n.format("betterquesting.btn.show") + ": " + quest.visibility.toString();
			SendChanges();
		}
	}

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
	@Override
    protected void keyTyped(char character, int keyCode)
    {
        super.keyTyped(character, keyCode);
        
        titleField.textboxKeyTyped(character, keyCode);
        descField.textboxKeyTyped(character, keyCode);
    }
	
    /**
     * Called when the mouse is clicked.
     */
	@Override
    protected void mouseClicked(int mx, int my, int click)
    {
		super.mouseClicked(mx, my, click);
		
		titleField.mouseClicked(mx, my, click);
		descField.mouseClicked(mx, my, click);
		
		boolean flag = false; // Just in case measure to prevent multiple update calls
		
		if(!titleField.isFocused() && !titleField.getText().equals(quest.getUnlocalisedName()))
		{
			// Apply changes, this way is automatic and doesn't require pressing Enter
			quest.name = titleField.getText();
			flag = true;
		}
		
		if(!descField.isFocused() && !descField.getText().equals(quest.getUnlocalisedDescription()))
		{
			// Apply changes, this way is automatic and doesn't require pressing Enter
			quest.description = descField.getText();
			flag = true;
		}
		
		if(flag)
		{
			SendChanges();
		}
    }
	
	// If the changes are approved by the server, it will be broadcast to all players including the editor
	public void SendChanges()
	{
		JsonObject json1 = new JsonObject();
		quest.writeToJson(json1, EnumSaveType.CONFIG);
		JsonObject json2 = new JsonObject();
		quest.writeToJson(json2, EnumSaveType.PROGRESS);
		NBTTagCompound tags = new NBTTagCompound();
		tags.setInteger("action", 0); // Action: Update data
		tags.setInteger("questID", QuestDatabase.INSTANCE.getKey(quest));
		tags.setTag("Data", NBTConverter.JSONtoNBT_Object(json1, new NBTTagCompound()));
		tags.setTag("Progress", NBTConverter.JSONtoNBT_Object(json2, new NBTTagCompound()));
		PacketSender.INSTANCE.sendToServer(PacketTypeNative.QUEST_EDIT.GetLocation(), tags);
	}

	@Override
	public void setText(int id, String text)
	{
		if(id == 0)
		{
			if(descField != null)
			{
				descField.setText(text);
			}
			
			quest.description = text;
			SendChanges();
		}
	}
}
